/*
 * Copyright (c) Direction Interministérielle du Numérique
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.txt
 */

import { Injectable } from '@angular/core';
import { KeyPairService } from '../key-pair/key-pair.service';
import { SodiumService } from '../sodium/sodium.service';
import { TempEncryptedStorageService } from '../temp-encrypted-storage/temp-encrypted-storage.service';

const CHUNK_SIZE = 5 * 1024 * 1024; // 5 MB

export interface EncryptedFileResult {
  encryptedFile: File;
  originalSize: number;
}

export interface PliEncryptionResult {
  encryptedFiles: EncryptedFileResult[];
  pliAesKeyEncryptedForSender: Uint8Array;
  pliAesKeyEncryptedForRecipient1: Uint8Array;
  pliAesKeyEncryptedForRecipient2: Uint8Array;
}

type ByteArray = Uint8Array;

@Injectable({
  providedIn: 'root'
})
export class FileEncryptionService {
  constructor(
    private readonly keyPairService: KeyPairService,
    private readonly sodiumService: SodiumService,
    private readonly tempEncryptedStorageService: TempEncryptedStorageService
  ) { }

  /**
   * Chiffre les fichiers avec la clé du pli (secretstream XChaCha20-Poly1305)
   * et encapsule la clé pour expéditeur + 2 destinataires (crypto_box_seal X25519).
   */
  async encryptFilesWithPliKey(
    items: Array<File | { file: File; relativePath?: string }>
  ): Promise<PliEncryptionResult> {
    console.log(`start encryptFilesWithPliKey`);
    const sodium = await this.sodiumService.getSodium();
    const [publicSender, publicRecipient1, publicRecipient2] =
      await this.keyPairService.getPocPublicKeys();

    // Clé symétrique 32 B pour tout le pli
    const pliKey = sodium.crypto_secretstream_xchacha20poly1305_keygen();

    // Chiffrer chaque fichier par chunks avec secretstream
    const encryptedFiles: EncryptedFileResult[] = [];
    for (const item of items) {
      const file = item instanceof File ? item : item.file;
      const encryptedFile = await this.encryptFileWithSecretstream(file, pliKey);
      encryptedFiles.push({ encryptedFile, originalSize: file.size });
    }

    // Encapsuler la clé pli pour chaque destinataire (crypto_box_seal = X25519 anonyme)
    const result = {
      encryptedFiles,
      pliAesKeyEncryptedForSender: new Uint8Array(sodium.crypto_box_seal(pliKey, publicSender)),
      pliAesKeyEncryptedForRecipient1: new Uint8Array(sodium.crypto_box_seal(pliKey, publicRecipient1)),
      pliAesKeyEncryptedForRecipient2: new Uint8Array(sodium.crypto_box_seal(pliKey, publicRecipient2))
    };
    // Effacer la clé pli en clair dès qu'elle n'est plus nécessaire
    sodium.memzero(pliKey);
    console.log(`end encryptFilesWithPliKey`);
    return result;
  }

  /**
   * Déchiffre la clé pli (base64) avec la paire de clés X25519 du destinataire.
   */
  async unwrapPliKey(
    sealedBase64: string,
    publicKey: Uint8Array,
    privateKey: Uint8Array
  ): Promise<Uint8Array> {
    console.log(`start unwrapPliKey`);
    const sodium = await this.sodiumService.getSodium();
    const sealed = sodium.from_base64(sealedBase64, sodium.base64_variants.ORIGINAL);
    const pliKey = sodium.crypto_box_seal_open(sealed, publicKey, privateKey);
    if (!pliKey) {
      throw new Error('Impossible de déchiffrer la clé du pli (clé privée incorrecte)');
    }
    console.log(`end unwrapPliKey`);
    return new Uint8Array(pliKey);
  }

  /**
   * Chiffre un fichier par chunks de CHUNK_SIZE avec secretstream (XChaCha20-Poly1305).
   * Format : [24 B header][chunk_1 + 17 B]...[chunk_N + 17 B (TAG_FINAL)]
   * Le plaintext est lu progressivement
   */
  async encryptFileWithSecretstream(file: File, pliKey: Uint8Array): Promise<File> {
    if (await this.tempEncryptedStorageService.isOpfsAccessible()) {
      try {
        return await this.encryptFileWithSecretstreamToOpfs(file, pliKey);
      } catch (error) {
        console.error('encryptFileWithSecretstreamToOpfs error', error);
        if (!this.tempEncryptedStorageService.isStorageFallbackError(error)) {
          throw error;
        }
        console.warn('OPFS encryption fallback to in-memory mode', error);
      }
    }
    return this.encryptFileWithSecretstreamInMemory(file, pliKey);
  }

  async cleanupTemporaryEncryptedFiles(): Promise<void> {
    await this.tempEncryptedStorageService.cleanupTemporaryEncryptedFiles();
  }

  private async encryptFileWithSecretstreamToOpfs(
    file: File,
    pliKey: Uint8Array
  ): Promise<File> {
    const encryptedFile = await this.tempEncryptedStorageService.writeEncryptedFile(
      file.name,
      async (writeChunk) => {
        await this.encryptFileWithSecretstreamCore(file, pliKey, async (chunk) => {
          await writeChunk(chunk);
        });
      },
      file.size
    );
    if (!encryptedFile) {
      throw new Error('OPFS not available');
    }
    return encryptedFile;
  }

  private async encryptFileWithSecretstreamInMemory(file: File, pliKey: Uint8Array): Promise<File> {
    const parts: ByteArray[] = [];
    await this.encryptFileWithSecretstreamCore(file, pliKey, (chunk) => {
      parts.push(chunk);
    });
    return new File([new Blob(parts as BlobPart[], { type: 'application/octet-stream' })], file.name, {
      type: 'application/octet-stream',
      lastModified: Date.now()
    });
  }

  private async encryptFileWithSecretstreamCore(
    file: File,
    pliKey: Uint8Array,
    onEncryptedChunk: (chunk: ByteArray) => Promise<void> | void
  ): Promise<void> {
    console.log(`start encryptFileWithSecretstream`);
    const sodium = await this.sodiumService.getSodium();
    const { state, header } =
      sodium.crypto_secretstream_xchacha20poly1305_init_push(pliKey);

    await onEncryptedChunk(header); // 24 B header en tête
    const reader = file.stream().getReader();
    let buffer: ByteArray = new Uint8Array(0);
    let done = false;
    let wroteAtLeastOnePayloadChunk = false;

    while (!done) {
      const { value, done: streamDone } = await reader.read();
      done = streamDone;

      if (value) {
        buffer = this.appendBuffer(buffer, value);
      }

      const encryptedBatch = await this.encryptAvailableChunks(
        sodium,
        state,
        buffer,
        done,
        onEncryptedChunk
      );
      buffer = encryptedBatch.remainingBuffer;
      wroteAtLeastOnePayloadChunk = wroteAtLeastOnePayloadChunk || encryptedBatch.wrotePayload;
    }

    // Cas fichier vide : émettre un chunk FINAL vide
    if (!wroteAtLeastOnePayloadChunk) {
      await onEncryptedChunk(sodium.crypto_secretstream_xchacha20poly1305_push(
        state, new Uint8Array(0), null,
        sodium.crypto_secretstream_xchacha20poly1305_TAG_FINAL
      ));
    }
    console.log(`end encryptFileWithSecretstream`);
  }

  private appendBuffer(current: ByteArray, incoming: ByteArray): ByteArray {
    const merged = new Uint8Array(current.byteLength + incoming.byteLength);
    merged.set(current);
    merged.set(incoming, current.byteLength);
    return merged;
  }

  private async encryptAvailableChunks(
    sodium: any,
    state: any,
    sourceBuffer: ByteArray,
    streamDone: boolean,
    onEncryptedChunk: (chunk: ByteArray) => Promise<void> | void
  ): Promise<{ remainingBuffer: ByteArray; wrotePayload: boolean }> {
    let buffer = sourceBuffer;
    let wrotePayload = false;
    while (buffer.byteLength >= CHUNK_SIZE || (streamDone && buffer.byteLength > 0)) {
      const isLast = streamDone && buffer.byteLength <= CHUNK_SIZE;
      const chunkEnd = isLast ? buffer.byteLength : CHUNK_SIZE;
      const chunk = buffer.slice(0, chunkEnd);
      const tag = isLast
        ? sodium.crypto_secretstream_xchacha20poly1305_TAG_FINAL
        : sodium.crypto_secretstream_xchacha20poly1305_TAG_MESSAGE;
      await onEncryptedChunk(
        sodium.crypto_secretstream_xchacha20poly1305_push(state, chunk, null, tag)
      );
      wrotePayload = true;
      buffer = buffer.slice(chunkEnd);
      if (isLast) {
        break;
      }
    }
    return { remainingBuffer: buffer, wrotePayload };
  }

  /**
   * Crée un TransformStream qui déchiffre un flux secretstream chunk par chunk.
   * Consomme le stream chiffré du backend et émet des Uint8Array en clair.
   *
   * Format attendu : [24 B header][CHUNK_SIZE + 17 B]...[dernierChunk + 17 B (TAG_FINAL)]
   */
  createDecryptTransformStream(pliKey: Uint8Array): TransformStream<Uint8Array, Uint8Array> {
    console.log(`start createDecryptTransformStream`);
    const HEADER_SIZE = 24; // crypto_secretstream_xchacha20poly1305_HEADERBYTES
    const ABYTES = 17;      // crypto_secretstream_xchacha20poly1305_ABYTES
    const ENCRYPTED_CHUNK_SIZE = CHUNK_SIZE + ABYTES;

    let state: any = null;
    let headerRead = false;
    let buffer: Uint8Array = new Uint8Array(0);
    let sodiumInstance: any = null;
    const sodiumService = this.sodiumService;

    const appendBuffer = (a: Uint8Array, b: Uint8Array): Uint8Array => {
      const merged = new Uint8Array(a.byteLength + b.byteLength);
      merged.set(a);
      merged.set(b, a.byteLength);
      return merged;
    };

    const decryptChunk = (enc: Uint8Array): Uint8Array => {
      const result = sodiumInstance.crypto_secretstream_xchacha20poly1305_pull(state, enc, null);
      if (!result) {
        throw new Error('Déchiffrement échoué : chunk corrompu ou clé incorrecte');
      }
      return new Uint8Array(result.message);
    };

    return new TransformStream<Uint8Array, Uint8Array>({
      async start() {
        sodiumInstance = await sodiumService.getSodium();
      },

      transform(chunk: Uint8Array, controller: TransformStreamDefaultController<Uint8Array>) {
        buffer = appendBuffer(buffer, new Uint8Array(chunk));

        // 1. Lire le header secretstream (24 B)
        if (!headerRead && buffer.byteLength >= HEADER_SIZE) {
          const header = buffer.slice(0, HEADER_SIZE);
          state = sodiumInstance.crypto_secretstream_xchacha20poly1305_init_pull(header, pliKey);
          buffer = buffer.slice(HEADER_SIZE);
          headerRead = true;
        }

        // 2. Déchiffrer les chunks complets (CHUNK_SIZE + 17 B)
        while (headerRead && buffer.byteLength >= ENCRYPTED_CHUNK_SIZE) {
          const encChunk = buffer.slice(0, ENCRYPTED_CHUNK_SIZE);
          controller.enqueue(decryptChunk(encChunk));
          buffer = buffer.slice(ENCRYPTED_CHUNK_SIZE);
        }
      },

      flush(controller: TransformStreamDefaultController<Uint8Array>) {
        // Dernier chunk (taille < ENCRYPTED_CHUNK_SIZE = dernier chunk + TAG_FINAL)
        if (headerRead && buffer.byteLength > 0) {
          controller.enqueue(decryptChunk(buffer));
        }
        // Effacer la clé pli et le buffer résiduel de la mémoire (sodium_memzero)
        console.log(`end createDecryptTransformStream`);
        sodiumInstance.memzero(pliKey);
        sodiumInstance.memzero(buffer);
      }
    });
  }

}

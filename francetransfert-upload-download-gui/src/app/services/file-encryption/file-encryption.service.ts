/*
 * Copyright (c) Direction Interministérielle du Numérique
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.txt
 */

import { Injectable } from '@angular/core';
import { SodiumService } from '../sodium/sodium.service';
import { TempEncryptedStorageService } from '../temp-encrypted-storage/temp-encrypted-storage.service';
import { LoggingService } from '../logging/logging.service';
import { take } from 'rxjs';

const CHUNK_SIZE = 5 * 1024 * 1024; // 5 MB
const PERF_LOG_PREFIX = '[ft-perf]';

export interface EncryptedFileResult {
  encryptedFile: File;
  originalSize: number;
}

export interface PliEncryptionResult {
  /** Single encrypted blob (cliphertext of the client-zip of all inputs). */
  encryptedFile: EncryptedFileResult;
  /** Symmetric pli key in raw bytes — to base64url-encode and put in URL fragment. */
  pliKey: Uint8Array;
}

type ByteArray = Uint8Array;

@Injectable({
  providedIn: 'root'
})
export class FileEncryptionService {
  constructor(
    private readonly sodiumService: SodiumService,
    private readonly tempEncryptedStorageService: TempEncryptedStorageService,
    private readonly loggingService: LoggingService
  ) { }

  private nowMs(): number {
    return typeof performance !== 'undefined' && typeof performance.now === 'function'
      ? performance.now()
      : Date.now();
  }

  /** Generate a fresh 32-byte pli key. */
  async generatePliKey(): Promise<Uint8Array> {
    const sodium = await this.sodiumService.getSodium();
    return sodium.crypto_secretstream_xchacha20poly1305_keygen();
  }

  /** Base64url-encode (no padding) for URL fragment transport. */
  async exportPliKey(pliKey: Uint8Array): Promise<string> {
    const sodium = await this.sodiumService.getSodium();
    return sodium.to_base64(pliKey, sodium.base64_variants.URLSAFE_NO_PADDING);
  }

  /** Decode a base64url-encoded pli key from URL fragment. */
  async importPliKey(b64url: string): Promise<Uint8Array> {
    const sodium = await this.sodiumService.getSodium();
    return new Uint8Array(sodium.from_base64(b64url, sodium.base64_variants.URLSAFE_NO_PADDING));
  }

  /**
   * Encrypt a single input File (already prepared by the caller — typically a
   * zip stream wrapping the original files) into one ciphertext File.
   */
  async encryptFileWithPliKey(
    file: File,
    pliKey: Uint8Array,
    onProgress?: (progress: number) => void
  ): Promise<PliEncryptionResult> {
    const totalStart = this.nowMs();
    const totalBytes = Math.max(file.size, 1);
    let encryptedBytes = 0;
    onProgress?.(0);
    const encryptedFile = await this.encryptFileWithSecretstream(
      file,
      pliKey,
      (plainChunkBytes) => {
        encryptedBytes += plainChunkBytes;
        onProgress?.(Math.min(100, Math.round((encryptedBytes / totalBytes) * 100)));
      }
    );
    onProgress?.(100);
    const durationMs = Math.round(this.nowMs() - totalStart);
    console.log(PERF_LOG_PREFIX, 'encryptFileWithPliKey.total', { durationMs, fileName: file.name, bytes: file.size });
    this.loggingService.logInfo(`encrypt duration: ${durationMs}ms file: ${file.name} bytes ${file.size}`)
      .pipe(take(1)).subscribe();
    return {
      encryptedFile: { encryptedFile, originalSize: file.size },
      pliKey
    };
  }

  /**
   * Chiffre un fichier par chunks de CHUNK_SIZE avec secretstream (XChaCha20-Poly1305).
   * Format : [24 B header][chunk_1 + 17 B]...[chunk_N + 17 B (TAG_FINAL)]
   * Le plaintext est lu progressivement
   */
  async encryptFileWithSecretstream(
    file: File,
    pliKey: Uint8Array,
    onPlainChunkEncrypted?: (plainChunkBytes: number) => void
  ): Promise<File> {
    if (await this.tempEncryptedStorageService.isOpfsAccessible()) {
      try {
        return await this.encryptFileWithSecretstreamToOpfs(file, pliKey, onPlainChunkEncrypted);
      } catch (error) {
        console.error('encryptFileWithSecretstreamToOpfs error', error);
        if (!this.tempEncryptedStorageService.isStorageFallbackError(error)) {
          throw error;
        }
        console.warn('OPFS encryption fallback to in-memory mode', error);
      }
    }
    return this.encryptFileWithSecretstreamInMemory(file, pliKey, onPlainChunkEncrypted);
  }

  async cleanupTemporaryEncryptedFiles(): Promise<void> {
    await this.tempEncryptedStorageService.cleanupTemporaryEncryptedFiles();
  }

  private async encryptFileWithSecretstreamToOpfs(
    file: File,
    pliKey: Uint8Array,
    onPlainChunkEncrypted?: (plainChunkBytes: number) => void
  ): Promise<File> {
    const encryptedFile = await this.tempEncryptedStorageService.writeEncryptedFile(
      file.name,
      async (writeChunk) => {
        await this.encryptFileWithSecretstreamCore(file, pliKey, async (chunk) => {
          await writeChunk(chunk);
        }, onPlainChunkEncrypted);
      },
      file.size
    );
    if (!encryptedFile) {
      throw new Error('OPFS not available');
    }
    return encryptedFile;
  }

  private async encryptFileWithSecretstreamInMemory(
    file: File,
    pliKey: Uint8Array,
    onPlainChunkEncrypted?: (plainChunkBytes: number) => void
  ): Promise<File> {
    const parts: ByteArray[] = [];
    await this.encryptFileWithSecretstreamCore(file, pliKey, (chunk) => {
      parts.push(chunk);
    }, onPlainChunkEncrypted);
    return new File([new Blob(parts as BlobPart[], { type: 'application/octet-stream' })], file.name, {
      type: 'application/octet-stream',
      lastModified: Date.now()
    });
  }

  private async encryptFileWithSecretstreamCore(
    file: File,
    pliKey: Uint8Array,
    onEncryptedChunk: (chunk: ByteArray) => Promise<void> | void,
    onPlainChunkEncrypted?: (plainChunkBytes: number) => void
  ): Promise<void> {
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
        onEncryptedChunk,
        onPlainChunkEncrypted
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
    onEncryptedChunk: (chunk: ByteArray) => Promise<void> | void,
    onPlainChunkEncrypted?: (plainChunkBytes: number) => void
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
      onPlainChunkEncrypted?.(chunkEnd);
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
    const HEADER_SIZE = 24; // crypto_secretstream_xchacha20poly1305_HEADERBYTES
    const ABYTES = 17;      // crypto_secretstream_xchacha20poly1305_ABYTES
    const ENCRYPTED_CHUNK_SIZE = CHUNK_SIZE + ABYTES;

    let state: any = null;
    let headerRead = false;
    let queuedChunks: Uint8Array[] = [];
    let queuedBytes = 0;
    let sodiumInstance: any = null;
    const sodiumService = this.sodiumService;
    const decryptStart = this.nowMs();
    let pulledChunks = 0;
    let pulledPlainBytes = 0;

    const queueChunk = (chunk: Uint8Array): void => {
      if (chunk.byteLength === 0) {
        return;
      }
      queuedChunks.push(chunk);
      queuedBytes += chunk.byteLength;
    };

    const readQueuedBytes = (size: number): Uint8Array => {
      const result = new Uint8Array(size);
      let offset = 0;

      while (offset < size && queuedChunks.length > 0) {
        const head = queuedChunks[0];
        const remaining = size - offset;
        if (head.byteLength <= remaining) {
          result.set(head, offset);
          offset += head.byteLength;
          queuedChunks.shift();
        } else {
          result.set(head.subarray(0, remaining), offset);
          queuedChunks[0] = head.subarray(remaining);
          offset += remaining;
        }
      }

      queuedBytes -= size;
      return result;
    };

    const decryptChunk = (enc: Uint8Array): Uint8Array => {
      const result = sodiumInstance.crypto_secretstream_xchacha20poly1305_pull(state, enc, null);
      if (!result) {
        throw new Error('Déchiffrement échoué : chunk corrompu ou clé incorrecte');
      }
      pulledChunks += 1;
      pulledPlainBytes += result.message?.byteLength ?? 0;
      return new Uint8Array(result.message);
    };

    return new TransformStream<Uint8Array, Uint8Array>({
      async start() {
        sodiumInstance = await sodiumService.getSodium();
      },

      transform(chunk: Uint8Array, controller: TransformStreamDefaultController<Uint8Array>) {
        queueChunk(new Uint8Array(chunk));

        if (!headerRead && queuedBytes >= HEADER_SIZE) {
          const header = readQueuedBytes(HEADER_SIZE);
          state = sodiumInstance.crypto_secretstream_xchacha20poly1305_init_pull(header, pliKey);
          headerRead = true;
        }

        while (headerRead && queuedBytes >= ENCRYPTED_CHUNK_SIZE) {
          const encChunk = readQueuedBytes(ENCRYPTED_CHUNK_SIZE);
          controller.enqueue(decryptChunk(encChunk));
        }
      },

      flush(controller: TransformStreamDefaultController<Uint8Array>) {
        if (headerRead && queuedBytes > 0) {
          controller.enqueue(decryptChunk(readQueuedBytes(queuedBytes)));
        }
        try {
          const durationMs = (typeof performance !== 'undefined' && typeof performance.now === 'function')
            ? performance.now() - decryptStart
            : Date.now() - decryptStart;
          console.log(PERF_LOG_PREFIX, 'decrypt.secretstream.total', {
            durationMs: Math.round(durationMs),
            pulledChunks,
            plainBytes: pulledPlainBytes
          });
        } catch {
          // ignore perf logging failures
        }
        sodiumInstance.memzero(pliKey);
        queuedChunks = [];
        queuedBytes = 0;
      }
    });
  }
}

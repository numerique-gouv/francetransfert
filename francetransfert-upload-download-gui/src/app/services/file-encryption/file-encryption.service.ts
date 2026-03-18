/*
 * Copyright (c) Direction Interministérielle du Numérique
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.txt
 */

import { Injectable } from '@angular/core';
import { KeyPairService } from '../key-pair/key-pair.service';

const AES_GCM = 'AES-GCM';
const RSA_OAEP = 'RSA-OAEP';
const AES_IV_LENGTH = 12;
const CHUNK_SIZE = 5 * 1024 * 1024; // 5 MB
const ENCRYPTED_CHUNK_SIZE = CHUNK_SIZE + AES_IV_LENGTH + 16; // IV + ciphertext + GCM tag

export interface EncryptedFileResult {
  encryptedFile: File;
  originalSize: number;
}

export interface PliEncryptionResult {
  encryptedFiles: EncryptedFileResult[];
  pliAesKeyEncryptedForSender: ArrayBuffer;
  pliAesKeyEncryptedForRecipient1: ArrayBuffer;
  pliAesKeyEncryptedForRecipient2: ArrayBuffer;
}

@Injectable({
  providedIn: 'root'
})
export class FileEncryptionService {
  constructor(private readonly keyPairService: KeyPairService) {}

  /**
   * Chiffre les fichiers avec la clé du pli et les envoie au serveur.
   * @param items Les fichiers à chiffrer.
   * @returns Les fichiers chiffrés.
   */
  async encryptFilesWithPliKey(
    items: Array<File | { file: File; relativePath?: string }>
  ): Promise<PliEncryptionResult> {
    const [publicSender, publicRecipient1, publicRecipient2] = await this.keyPairService.getPocPublicKeys();

    // Clé AES pour tout le pli
    const pliAesKey = await globalThis.crypto.subtle.generateKey(
      { name: AES_GCM, length: 256 },
      true,
      ['encrypt']
    );

    // Chiffrer chaque fichier par chunks avec la clé du pli
    const encryptedFiles: EncryptedFileResult[] = [];
    for (const item of items) {
      const file = typeof item === 'object' && 'file' in item ? item.file : item;
      const relativePath = typeof item === 'object' && 'relativePath' in item ? item.relativePath : undefined;
      const fileBuffer = await file.arrayBuffer();
      const encryptedBuffer = await this.encryptFileInChunks(fileBuffer, pliAesKey);
      const encryptedBlob = new Blob([encryptedBuffer], { type: 'application/octet-stream' });
      const fileOptions: FilePropertyBag = { type: 'application/octet-stream', lastModified: Date.now() };
      if (relativePath !== undefined && relativePath !== '') {
        (fileOptions as FilePropertyBag & { webkitRelativePath?: string }).webkitRelativePath = relativePath;
      }
      const encryptedFile = new File([encryptedBlob], file.name, fileOptions);
      encryptedFiles.push({ encryptedFile, originalSize: file.size });
    }

    // Une seule clé AES du pli, chiffrée avec la clé publique de l'expéditeur et des destinataires (RSA-OAEP)
    const [encryptedForSender, encryptedForR1, encryptedForR2] = await Promise.all([
      globalThis.crypto.subtle.wrapKey('raw', pliAesKey, publicSender, { name: RSA_OAEP }),
      globalThis.crypto.subtle.wrapKey('raw', pliAesKey, publicRecipient1, { name: RSA_OAEP }),
      globalThis.crypto.subtle.wrapKey('raw', pliAesKey, publicRecipient2, { name: RSA_OAEP })
    ]);

    return {
      encryptedFiles,
      pliAesKeyEncryptedForSender: encryptedForSender,
      pliAesKeyEncryptedForRecipient1: encryptedForR1,
      pliAesKeyEncryptedForRecipient2: encryptedForR2
    };
  }

  // Déchiffre la clé du pli (base64) avec la clé privée RSA (expéditeur ou destinataire).
  async unwrapPliKey(encryptedPliKeyBase64: string, privateKey: CryptoKey): Promise<CryptoKey> {
    const binary = atob(encryptedPliKeyBase64);
    const wrapped = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      wrapped[i] = binary.charCodeAt(i);
    }
    return globalThis.crypto.subtle.unwrapKey(
      'raw',
      wrapped.buffer,
      privateKey,
      { name: RSA_OAEP },
      { name: AES_GCM, length: 256 },
      true,
      ['decrypt']
    );
  }

  /**
   * Chiffre un fichier par chunks de CHUNK_SIZE.
   * Format : [4 octets : nb chunks (uint32 big-endian)][IV 12 oct][ciphertext+tag]...(× nb chunks)
   */
  async encryptFileInChunks(fileBuffer: ArrayBuffer, pliAesKey: CryptoKey): Promise<ArrayBuffer> {
    const chunkCount = Math.ceil(fileBuffer.byteLength / CHUNK_SIZE) || 1;
    console.log('chunkCount', chunkCount);
    const header = new Uint8Array(4);
    new DataView(header.buffer).setUint32(0, chunkCount, false);
    const parts: Uint8Array[] = [header];

    for (let i = 0; i < chunkCount; i++) {
      const chunk = fileBuffer.slice(i * CHUNK_SIZE, (i + 1) * CHUNK_SIZE);
      const iv = globalThis.crypto.getRandomValues(new Uint8Array(AES_IV_LENGTH));
      const cipher = await globalThis.crypto.subtle.encrypt(
        { name: AES_GCM, iv, tagLength: 128 }, pliAesKey, chunk
      );
      const out = new Uint8Array(AES_IV_LENGTH + cipher.byteLength);
      out.set(iv);
      out.set(new Uint8Array(cipher), AES_IV_LENGTH);
      parts.push(out);
    }

    const total = parts.reduce((s, p) => s + p.byteLength, 0);
    const result = new Uint8Array(total);
    let offset = 0;
    for (const p of parts) { result.set(p, offset); offset += p.byteLength; }
    return result.buffer;
  }

  /**
   * Déchiffre un fichier chiffré par chunks.
   * Format attendu : [4 octets : nb chunks][IV 12 oct][ciphertext+tag]...(× nb chunks)
   */
  async decryptFileInChunks(encryptedBuffer: ArrayBuffer, pliAesKey: CryptoKey): Promise<ArrayBuffer> {
    if (encryptedBuffer.byteLength < 4) {
      throw new Error('Fichier chiffré invalide (header manquant)');
    }
    const chunkCount = new DataView(encryptedBuffer).getUint32(0, false);
    console.log(`[decryptFileInChunks] chunkCount=${chunkCount}, bufferSize=${encryptedBuffer.byteLength}`);
    const parts: ArrayBuffer[] = [];

    for (let i = 0; i < chunkCount; i++) {
      const start = 4 + i * ENCRYPTED_CHUNK_SIZE;
      const iv = encryptedBuffer.slice(start, start + AES_IV_LENGTH);
      const cipher = encryptedBuffer.slice(start + AES_IV_LENGTH, start + ENCRYPTED_CHUNK_SIZE);
      console.log(`[decryptFileInChunks] chunk ${i}/${chunkCount} start=${start} cipherLen=${cipher.byteLength}`);
      const plain = await globalThis.crypto.subtle.decrypt(
        { name: AES_GCM, iv, tagLength: 128 }, pliAesKey, cipher
      );
      parts.push(plain);
    }

    const total = parts.reduce((s, p) => s + p.byteLength, 0);
    const result = new Uint8Array(total);
    let offset = 0;
    for (const p of parts) { result.set(new Uint8Array(p), offset); offset += p.byteLength; }
    return result.buffer;
  }
}

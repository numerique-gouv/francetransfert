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

    // Chiffrer chaque fichier avec la clé du pli (IV différent par fichier)
    const encryptedFiles: EncryptedFileResult[] = [];
    for (const item of items) {
      const file = typeof item === 'object' && 'file' in item ? item.file : item;
      const relativePath = typeof item === 'object' && 'relativePath' in item ? item.relativePath : undefined;
      const fileBuffer = await file.arrayBuffer();
      const iv = globalThis.crypto.getRandomValues(new Uint8Array(AES_IV_LENGTH));
      const ciphertext = await globalThis.crypto.subtle.encrypt(
        { name: AES_GCM, iv, tagLength: 128 },
        pliAesKey,
        fileBuffer
      );
      const out = new Uint8Array(AES_IV_LENGTH + ciphertext.byteLength);
      out.set(iv, 0);
      out.set(new Uint8Array(ciphertext), AES_IV_LENGTH);
      const encryptedBlob = new Blob([out], { type: 'application/octet-stream' });
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
   * Déchiffre le contenu d'un fichier (format [IV 12 octets][ciphertext AES-GCM]) avec la clé du pli.
   */
  async decryptFileContent(encryptedFileBuffer: ArrayBuffer, pliAesKey: CryptoKey): Promise<ArrayBuffer> {
    if (encryptedFileBuffer.byteLength < AES_IV_LENGTH) {
      throw new Error('Fichier chiffré trop court (IV manquant)');
    }
    const iv = encryptedFileBuffer.slice(0, AES_IV_LENGTH);
    const ciphertext = encryptedFileBuffer.slice(AES_IV_LENGTH);
    return globalThis.crypto.subtle.decrypt(
      { name: AES_GCM, iv, tagLength: 128 },
      pliAesKey,
      ciphertext
    );
  }
}

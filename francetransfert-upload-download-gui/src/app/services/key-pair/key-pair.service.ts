/*
 * Copyright (c) Direction Interministérielle du Numérique
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.txt
 */

import { Injectable } from '@angular/core';
import { IndexedDbService } from '../indexed-db/indexed-db.service';

/** Clés d'enrôlement persistées pour expéditeur et destinataires (même navigateur). */
const POC_SENDER_PUBLIC = 'poc_sender_public';
const POC_SENDER_PRIVATE = 'poc_sender_private';
const POC_RECIPIENT1_PUBLIC = 'poc_recipient1_public';
const POC_RECIPIENT1_PRIVATE = 'poc_recipient1_private';
const POC_RECIPIENT2_PUBLIC = 'poc_recipient2_public';
const POC_RECIPIENT2_PRIVATE = 'poc_recipient2_private';

/** Algorithme RSA-OAEP, 2048 bits, utilisable pour chiffrement et signature. */
const KEY_ALGORITHM: RsaHashedKeyGenParams = {
  name: 'RSA-OAEP',
  modulusLength: 2048,
  publicExponent: new Uint8Array([1, 0, 1]),
  hash: 'SHA-256'
};

export interface StoredKeyPair {
  publicKey: CryptoKey;
  privateKey: CryptoKey;
}

/** Simule l'enrôlement déjà fait (1 expéditeur + 2 destinataires). */
export interface PocEnrollmentKeyPairs {
  sender: StoredKeyPair;
  recipient1: StoredKeyPair;
  recipient2: StoredKeyPair;
}

@Injectable({
  providedIn: 'root'
})
export class KeyPairService {

  private pocEnrollmentKeyPairs: PocEnrollmentKeyPairs | null = null;

  constructor(private readonly indexedDb: IndexedDbService) {}

  /**
   * Génère une paire de clés RSA.
   * @returns La paire de clés.
   */
  async generateKeyPair(): Promise<StoredKeyPair> {
    const pair = await globalThis.crypto.subtle.generateKey(
      KEY_ALGORITHM,
      true,
      ['encrypt', 'decrypt', 'wrapKey', 'unwrapKey']
    );
    return {
      publicKey: pair.publicKey,
      privateKey: pair.privateKey
    };
  }

  /**
   * Charge les paires de clés depuis IndexedDB ou les génère et les persiste.
   * @returns Les paires de clés.
   */
  async getPocEnrollmentKeyPairs(): Promise<PocEnrollmentKeyPairs> {
    if (this.pocEnrollmentKeyPairs) {
      return this.pocEnrollmentKeyPairs;
    }
    const loaded = await this.loadPocKeyPairsFromStorage();
    if (loaded) {
      this.pocEnrollmentKeyPairs = loaded;
      return this.pocEnrollmentKeyPairs;
    }
    const [sender, recipient1, recipient2] = await Promise.all([
      this.generateKeyPair(),
      this.generateKeyPair(),
      this.generateKeyPair()
    ]);
    this.pocEnrollmentKeyPairs = { sender, recipient1, recipient2 };
    await this.persistPocKeyPairs(this.pocEnrollmentKeyPairs);
    return this.pocEnrollmentKeyPairs;
  }

  /**
   * Charge les paires de clés depuis IndexedDB.
   * @returns Les paires de clés.
   */
  private async loadPocKeyPairsFromStorage(): Promise<PocEnrollmentKeyPairs | null> {
    const [sp, sv, r1p, r1v, r2p, r2v] = await Promise.all([
      this.indexedDb.get<JsonWebKey>(POC_SENDER_PUBLIC),
      this.indexedDb.get<JsonWebKey>(POC_SENDER_PRIVATE),
      this.indexedDb.get<JsonWebKey>(POC_RECIPIENT1_PUBLIC),
      this.indexedDb.get<JsonWebKey>(POC_RECIPIENT1_PRIVATE),
      this.indexedDb.get<JsonWebKey>(POC_RECIPIENT2_PUBLIC),
      this.indexedDb.get<JsonWebKey>(POC_RECIPIENT2_PRIVATE)
    ]);
    if (!sp || !sv || !r1p || !r1v || !r2p || !r2v) {
      return null;
    }

    /**
     * Importe une paire de clés RSA.
     * @param publicJwk La clé publique.
     * @param privateJwk La clé privée.
     * @returns La paire de clés.
     */
    const importPair = async (publicJwk: JsonWebKey, privateJwk: JsonWebKey): Promise<StoredKeyPair> => {
      const publicKey = await globalThis.crypto.subtle.importKey('jwk', publicJwk, KEY_ALGORITHM, true, ['encrypt', 'wrapKey']);
      const privateKey = await globalThis.crypto.subtle.importKey('jwk', privateJwk, KEY_ALGORITHM, true, ['decrypt', 'unwrapKey']);
      return { publicKey, privateKey };
    };
    const [sender, recipient1, recipient2] = await Promise.all([
      importPair(sp, sv),
      importPair(r1p, r1v),
      importPair(r2p, r2v)
    ]);
    return { sender, recipient1, recipient2 };
  }

  /**
   * Persiste les paires de clés dans IndexedDB.
   * @param pairs Les paires de clés.
   */
  private async persistPocKeyPairs(pairs: PocEnrollmentKeyPairs): Promise<void> {
    const toJwk = async (key: CryptoKey) => globalThis.crypto.subtle.exportKey('jwk', key);
    const [sp, sv, r1p, r1v, r2p, r2v] = await Promise.all([
      toJwk(pairs.sender.publicKey),
      toJwk(pairs.sender.privateKey),
      toJwk(pairs.recipient1.publicKey),
      toJwk(pairs.recipient1.privateKey),
      toJwk(pairs.recipient2.publicKey),
      toJwk(pairs.recipient2.privateKey)
    ]);
    await Promise.all([
      this.indexedDb.set(POC_SENDER_PUBLIC, sp as object),
      this.indexedDb.set(POC_SENDER_PRIVATE, sv as object),
      this.indexedDb.set(POC_RECIPIENT1_PUBLIC, r1p as object),
      this.indexedDb.set(POC_RECIPIENT1_PRIVATE, r1v as object),
      this.indexedDb.set(POC_RECIPIENT2_PUBLIC, r2p as object),
      this.indexedDb.set(POC_RECIPIENT2_PRIVATE, r2v as object)
    ]);
  }

  /**
   * Retourne les clés publiques des paires de clés.
   * @returns Les clés publiques.
   */
  async getPocPublicKeys(): Promise<[CryptoKey, CryptoKey, CryptoKey]> {
    const pairs = await this.getPocEnrollmentKeyPairs();
    return [pairs.sender.publicKey, pairs.recipient1.publicKey, pairs.recipient2.publicKey];
  }
}

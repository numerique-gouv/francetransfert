/*
 * Copyright (c) Direction Interministérielle du Numérique
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.txt
 */

import { Injectable } from '@angular/core';
import { IndexedDbService } from '../indexed-db/indexed-db.service';
import { SodiumService } from '../sodium/sodium.service';

/** Clés d'enrôlement persistées pour expéditeur et destinataires (même navigateur). */
const POC_SENDER_PUBLIC = 'poc_sender_public';
const POC_SENDER_PRIVATE = 'poc_sender_private';
const POC_RECIPIENT1_PUBLIC = 'poc_recipient1_public';
const POC_RECIPIENT1_PRIVATE = 'poc_recipient1_private';
const POC_RECIPIENT2_PUBLIC = 'poc_recipient2_public';
const POC_RECIPIENT2_PRIVATE = 'poc_recipient2_private';

/** Paire de clés X25519 (32 octets chacune). */
export interface StoredKeyPair {
  publicKey: Uint8Array;
  privateKey: Uint8Array;
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

  constructor(
    private readonly indexedDb: IndexedDbService,
    private readonly sodiumService: SodiumService
  ) {}

  /**
   * Génère une paire de clés X25519 via libsodium.
   */
  async generateKeyPair(): Promise<StoredKeyPair> {
    const sodium = await this.sodiumService.getSodium();
    const pair = sodium.crypto_box_keypair();
    return { publicKey: new Uint8Array(pair.publicKey), privateKey: new Uint8Array(pair.privateKey) };
  }

  /**
   * Charge les paires de clés depuis IndexedDB ou les génère et les persiste.
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
   * Charge les paires de clés depuis IndexedDB (stockées en base64).
   */
  private async loadPocKeyPairsFromStorage(): Promise<PocEnrollmentKeyPairs | null> {
    const sodium = await this.sodiumService.getSodium();
    const [sp, sv, r1p, r1v, r2p, r2v] = await Promise.all([
      this.indexedDb.get<string>(POC_SENDER_PUBLIC),
      this.indexedDb.get<string>(POC_SENDER_PRIVATE),
      this.indexedDb.get<string>(POC_RECIPIENT1_PUBLIC),
      this.indexedDb.get<string>(POC_RECIPIENT1_PRIVATE),
      this.indexedDb.get<string>(POC_RECIPIENT2_PUBLIC),
      this.indexedDb.get<string>(POC_RECIPIENT2_PRIVATE)
    ]);
    if (!sp || !sv || !r1p || !r1v || !r2p || !r2v) {
      return null;
    }
    const fromB64 = (b64: string): Uint8Array => new Uint8Array(sodium.from_base64(b64));
    return {
      sender:     { publicKey: fromB64(sp),  privateKey: fromB64(sv)  },
      recipient1: { publicKey: fromB64(r1p), privateKey: fromB64(r1v) },
      recipient2: { publicKey: fromB64(r2p), privateKey: fromB64(r2v) }
    };
  }

  /**
   * Persiste les paires de clés dans IndexedDB sous forme base64.
   */
  private async persistPocKeyPairs(pairs: PocEnrollmentKeyPairs): Promise<void> {
    const sodium = await this.sodiumService.getSodium();
    const toB64 = (key: Uint8Array) => sodium.to_base64(key);
    await Promise.all([
      this.indexedDb.set(POC_SENDER_PUBLIC,     toB64(pairs.sender.publicKey)),
      this.indexedDb.set(POC_SENDER_PRIVATE,    toB64(pairs.sender.privateKey)),
      this.indexedDb.set(POC_RECIPIENT1_PUBLIC, toB64(pairs.recipient1.publicKey)),
      this.indexedDb.set(POC_RECIPIENT1_PRIVATE,toB64(pairs.recipient1.privateKey)),
      this.indexedDb.set(POC_RECIPIENT2_PUBLIC, toB64(pairs.recipient2.publicKey)),
      this.indexedDb.set(POC_RECIPIENT2_PRIVATE,toB64(pairs.recipient2.privateKey))
    ]);
  }

  /**
   * Retourne les clés publiques X25519 des trois paires.
   */
  async getPocPublicKeys(): Promise<[Uint8Array, Uint8Array, Uint8Array]> {
    const pairs = await this.getPocEnrollmentKeyPairs();
    return [pairs.sender.publicKey, pairs.recipient1.publicKey, pairs.recipient2.publicKey];
  }
}

/*
 * Copyright (c) Direction Interministérielle du Numérique
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.txt
 */

import { Injectable } from '@angular/core';

const DB_NAME = 'FranceTransfertKeys';
const DB_VERSION = 1;
const STORE_NAME = 'keys';

@Injectable({
  providedIn: 'root'
})
export class IndexedDbService {
  private db: IDBDatabase | null = null;
  private initPromise: Promise<IDBDatabase> | null = null;

  /**
   * Ouvre la base IndexedDB et crée le store si nécessaire.
   */
  async openDb(): Promise<IDBDatabase> {
    if (this.db) {
      return this.db;
    }
    if (this.initPromise !== null) {
      return this.initPromise;
    }
    this.initPromise = new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, DB_VERSION);
      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        this.db = request.result;
        resolve(this.db);
      };
      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result;
        if (!db.objectStoreNames.contains(STORE_NAME)) {
          db.createObjectStore(STORE_NAME, { keyPath: 'id' });
        }
      };
    });
    return this.initPromise;
  }

  /**
   * Enregistre une valeur dans le store.
   * @param id Clé d'accès
   * @param value Valeur sérialisable (objet ou string)
   */
  async set<T extends object | string>(id: string, value: T): Promise<void> {
    const db = await this.openDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readwrite');
      const store = tx.objectStore(STORE_NAME);
      const request = store.put({ id, value });
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve();
    });
  }

  /**
   * Récupère une valeur du store.
   */
  async get<T>(id: string): Promise<T | null> {
    const db = await this.openDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readonly');
      const store = tx.objectStore(STORE_NAME);
      const request = store.get(id);
      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        const row = request.result;
        resolve(row ? (row.value as T) : null);
      };
    });
  }
}

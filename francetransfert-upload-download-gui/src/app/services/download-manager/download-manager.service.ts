/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs/internal/BehaviorSubject';
import {FTErrorModel, LinkInfosModel, MailInfosModel} from 'src/app/models';

@Injectable({
  providedIn: 'root'
})
export class DownloadManagerService {

  downloadError$: BehaviorSubject<FTErrorModel> = new BehaviorSubject<FTErrorModel>(null);
  pliAesKey: BehaviorSubject<Uint8Array | null> = new BehaviorSubject<Uint8Array | null>(null);

  constructor() { }

  setPliAesKey(key: Uint8Array | null): void {
    this.pliAesKey.next(key);
  }

  clearPliAesKey(): void {
    const current = this.pliAesKey.getValue();
    if (current) {
      current.fill(0); // sodium_memzero équivalent JS : écrase les octets avant libération
    }
    this.pliAesKey.next(null);
  }
}

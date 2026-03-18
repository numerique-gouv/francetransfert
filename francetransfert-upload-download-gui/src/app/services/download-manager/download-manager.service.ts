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
  pliAesKey: BehaviorSubject<CryptoKey | null> = new BehaviorSubject<CryptoKey | null>(null);

  constructor() { }

  setPliAesKey(key: CryptoKey | null): void {
    this.pliAesKey.next(key);
  }

  clearPliAesKey(): void {
    this.pliAesKey.next(null);
  }
}

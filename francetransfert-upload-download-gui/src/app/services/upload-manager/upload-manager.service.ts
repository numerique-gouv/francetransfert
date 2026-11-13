/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Injectable, signal } from '@angular/core';
import { Observable, filter, firstValueFrom } from 'rxjs';
import { FTErrorModel, LinkInfosModel, MailInfosModel, UploadInfosModel } from 'src/app/models';

@Injectable({
  providedIn: 'root'
})
export class UploadManagerService {

  readonly envelopeInfos = signal<MailInfosModel | LinkInfosModel | null>(null);
  readonly uploadError = signal<FTErrorModel | null>(null);
  readonly uploadInfos = signal<UploadInfosModel | null>(null);

  getRxValue(observable: Observable<any>): Promise<any> {
    return firstValueFrom(observable.pipe(filter(this.hasValue)));
  }

  hasValue(value: any) {
    return value !== null && value !== undefined;
  }
}

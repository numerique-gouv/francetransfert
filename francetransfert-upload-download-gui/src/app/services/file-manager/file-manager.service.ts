/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Injectable, signal } from '@angular/core';
import { UploadState } from '@flowjs/ngx-flow';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FileManagerService {

  readonly hasFiles = signal(false);
  readonly transfers = signal<Observable<UploadState> | null>(null);
  readonly uploadProgress = signal<UploadState | null>(null);

}

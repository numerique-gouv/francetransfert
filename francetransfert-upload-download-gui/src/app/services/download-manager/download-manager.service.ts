/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Injectable, signal } from '@angular/core';
import { FTErrorModel } from 'src/app/models';

@Injectable({
  providedIn: 'root'
})
export class DownloadManagerService {

  readonly downloadError = signal<FTErrorModel | null>(null);

}

/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { Injectable } from '@angular/core';
import { UploadState } from '@flowjs/ngx-flow';
import { BehaviorSubject } from 'rxjs/internal/BehaviorSubject';
import { Observable } from 'rxjs/internal/Observable';

@Injectable({
  providedIn: 'root'
})
export class FileManagerService {

  hasFiles: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  transfers: BehaviorSubject<Observable<UploadState>> = new BehaviorSubject<Observable<UploadState>>(null);
  uploadProgress: BehaviorSubject<UploadState> = new BehaviorSubject<UploadState>(null);

  constructor() { }
}

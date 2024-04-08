/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs/internal/BehaviorSubject';
import { Observable } from 'rxjs/internal/Observable';
import { filter } from 'rxjs/internal/operators/filter';
import { first } from 'rxjs/internal/operators/first';
import { FTErrorModel, LinkInfosModel, MailInfosModel, UploadInfosModel } from 'src/app/models';

@Injectable({
  providedIn: 'root'
})
export class UploadManagerService {

  envelopeInfos: BehaviorSubject<MailInfosModel | LinkInfosModel> = new BehaviorSubject<MailInfosModel | LinkInfosModel>(null);
  uploadError$: BehaviorSubject<FTErrorModel> = new BehaviorSubject<FTErrorModel>(null);
  uploadInfos: BehaviorSubject<UploadInfosModel> = new BehaviorSubject<UploadInfosModel>(null);

  constructor() { }

  getRxValue(observable: Observable<any>): Promise<any> {
    return observable.pipe(filter(this.hasValue), first()).toPromise();
  }

  hasValue(value: any) {
    return value !== null && value !== undefined;
  }
}

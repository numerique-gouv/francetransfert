/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { map, catchError, take } from 'rxjs/operators';
import { environment } from 'src/environments/environment';

export interface ConfigInfo {
  mimeType: string[];
  extension: string[];
  agentConnect: boolean;
  issuerUrl: string;
  clientId: string;
  messages: { [key: string]: string };
  uploadExpiredLimit: number;
  uploadMaxRecipientAgent: number;
  uploadMaxRecipientPublic: number;
};


@Injectable({
  providedIn: 'root'
})
export class ConfigService {

  configError$: BehaviorSubject<number> = new BehaviorSubject<number>(null);
  isAgentConnect: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  configInfo: BehaviorSubject<ConfigInfo> = new BehaviorSubject<ConfigInfo>(null);


  constructor(private _httpClient: HttpClient) {
    this.getConfig().pipe(take(1)).subscribe((res: ConfigInfo) => {
      this.configInfo.next(res);
      this.isAgentConnect.next(res.agentConnect);
    })
  }

  getMailLimit(isAgent: boolean) {
    if (isAgent) {
      return this.configInfo.getValue().uploadMaxRecipientAgent;
    } else {
      return this.configInfo.getValue().uploadMaxRecipientPublic;
    }
  }

  getConfig() {
    const httpOptions = {
      headers: new HttpHeaders(),
      withCredentials: false
    };
    return this._httpClient.get(
      `${environment.host}${environment.apis.upload.config}`, httpOptions
    ).pipe(map((response: ConfigInfo) => {
      this.configError$.next(null);
      this.configInfo.next(response);
      return response;
    }),
      catchError(this.handleError('getConfig'))
    );
  }

  private handleError(operation: string) {
    return (err: any) => {
      const errMsg = `error in ${operation}()`;
      if (err instanceof HttpErrorResponse) {
        this.configError$.next(err.status);
      }
      throw (errMsg);
    };
  }
}

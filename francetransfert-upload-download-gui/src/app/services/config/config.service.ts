/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
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
  private _httpClient = inject(HttpClient);


  readonly configError = signal<number | null>(null);
  readonly configInfo = signal<ConfigInfo | null>(null);
  readonly isAgentConnect = computed(() => !!this.configInfo()?.agentConnect);


  constructor() {
    this.getConfig().pipe(take(1)).subscribe();
  }

  getMailLimit(isAgent: boolean) {
    if (isAgent) {
      return this.configInfo().uploadMaxRecipientAgent;
    } else {
      return this.configInfo().uploadMaxRecipientPublic;
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
      this.configError.set(null);
      this.configInfo.set(response);
      return response;
    }),
      catchError(this.handleError('getConfig'))
    );
  }

  private handleError(operation: string) {
    return (err: any) => {
      const errMsg = `error in ${operation}()`;
      if (err instanceof HttpErrorResponse) {
        this.configError.set(err.status);
      }
      throw (errMsg);
    };
  }
}

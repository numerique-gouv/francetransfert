/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { catchError } from 'rxjs/internal/operators/catchError';
import { map } from 'rxjs/internal/operators/map';
import { environment } from 'src/environments/environment';
import { DownloadManagerService } from '../download-manager/download-manager.service';
import { LoginService } from '../login/login.service';

@Injectable({
  providedIn: 'root'
})
export class DownloadService {

  constructor(private _httpClient: HttpClient, private downloadManagerService: DownloadManagerService, private loginService: LoginService) { }

  getDownloadInfos(params: Array<{ string: string }>) {

    const body = {
      enclosure: params['enclosure'],
      recipient: params['recipient'],
      token: params['token'],
    };

    return this._httpClient.post(
      `${environment.host}${environment.apis.download.download}`,
      body
    ).pipe(map(response => {
      this.downloadManagerService.downloadError$.next(null);
      return response;
    }),
      catchError(this.handleError('download-info'))
    );
  }

  getDownloadUrl(params: Array<{ string: string }>, withPassword: boolean, password: string): Observable<any> {
    var escapedPassword = '';
    if (withPassword) {
      escapedPassword = password;
    }
    const body = {
      enclosure: params['enclosure'],
      ...params['recipient'] ? { recipient: params['recipient'] } : { recipient: this.loginService.tokenInfo.getValue().senderMail },
      token: params['token'],
      ...params['recipient'] ? { senderToken: null } : { senderToken: this.loginService.tokenInfo.getValue().senderToken },
      ...withPassword ? { password: escapedPassword } : { password: '' }
    };
    return this._httpClient.post(
      `${environment.host}${environment.apis.download.downloadUrl}`,
      body
    ).pipe(map(response => {
      this.downloadManagerService.downloadError$.next(null);
      return response;
    }),
      catchError(this.handleError('generate-download-url'))
    );
  }

  getDownloadInfosConnect(enclosureId: string, senderToken: string, recipient: string) {

    const body = {
      enclosure: enclosureId,
      token: senderToken,
      recipient: recipient,
    };

    return this._httpClient.post(
      `${environment.host}${environment.apis.download.downloadConnect}`,
      body
    ).pipe(map(response => {
      this.downloadManagerService.downloadError$.next(null);
      return response;
    }),
      catchError(this.handleError('download-info-connect'))
    );
  }


  validatePassword(_body: any): any {
    const body = {
      enclosureId: _body.enclosureId,
      password: _body.password,
      recipientId: _body.recipientId,
    };
    return this._httpClient.post(
      `${environment.host}${environment.apis.download.validatePassword}`,
      body
    ).pipe(map(response => {
      this.downloadManagerService.downloadError$.next(null);
      return response;
    }),
      catchError(this.handleError('validateCode'))
    );
  }

  getDownloadInfosPublic(params: Array<{ string: string }>) {
    return this._httpClient.get(
      `${environment.host}${environment.apis.download.downloadInfosPublic}?enclosure=${params['enclosure']}`
    ).pipe(map(response => {
      this.downloadManagerService.downloadError$.next(null);
      return response;
    }),
      catchError(this.handleError('download-info-public'))
    );
  }

  getDownloadUrlPublic(params: Array<{ string: string }>, password: string): Observable<any> {
    let escapedPassword = password;
    const body = {
      enclosure: params['enclosure'],
      password: escapedPassword
    };
    return this._httpClient.post(
      `${environment.host}${environment.apis.download.downloadUrlPublic}`,
      body
    ).pipe(map(response => {
      this.downloadManagerService.downloadError$.next(null);
      return response;
    }),
      catchError(this.handleError('generate-download-url-public'))
    );
  }

  rate(body: any): any {
    return this._httpClient.post(`${environment.host}${environment.apis.download.rate}`, {
      plis: body.plis,
      mailAdress: body.mail,
      message: body.message,
      satisfaction: body.satisfaction
    }).pipe(map(response => {
      this.downloadManagerService.downloadError$.next(null);
      return response;
    }),
      catchError(this.handleError('rate'))
    );
  }

  private handleError(operation: string) {
    return (err: any) => {
      const errMsg = `error in ${operation}()`;
      if (err instanceof HttpErrorResponse) {
        this.downloadManagerService.downloadError$.next({ statusCode: err.status, ...(err.message && !err.error.type) ? { message: err.message } : { message: err.error.type }, ...err.error.codeTryCount ? { codeTryCount: err.error.codeTryCount } : {} });
      }
      throw (errMsg);
    };
  }
}

/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { map, catchError, take } from 'rxjs/operators';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ConfigService {

  configError$: BehaviorSubject<number> = new BehaviorSubject<number>(null);
  isAgentConnect: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  configInfo: BehaviorSubject<any> = new BehaviorSubject<any>(null);


  constructor(private _httpClient: HttpClient) {
    this.getConfig().pipe(take(1)).subscribe((res: any) => {
      this.configInfo.next(res);
      this.isAgentConnect.next(res.agentConnect);
    })
  }

  getConfig() {
    const httpOptions = {
      headers: new HttpHeaders(),
      withCredentials: false
    };
    return this._httpClient.get(
      `${environment.host}${environment.apis.upload.config}`, httpOptions
    ).pipe(map(response => {
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

/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class LoggingService {

  constructor(private httpClient: HttpClient) { }

  logInfo(message: string): Observable<any> {
    return this.httpClient.post(`${environment.host}${environment.apis.logInfo}`, { message }).pipe(
      map((response: any) => {
        return response;
      }),
      catchError(this.handleError('logInfo'))
    );
  }


  private handleError(operation: string) {
    return (err: any) => {
      const errMsg = `error in ${operation}()`;
      console.error(err);
      throw (errMsg);
    };
  }

}

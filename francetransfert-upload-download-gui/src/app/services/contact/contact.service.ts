/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from "@angular/common/http";
import { environment } from "../../../environments/environment";
import { catchError, map } from "rxjs/operators";
import { UploadManagerService } from "../upload-manager/upload-manager.service";
import { ResponsiveService } from "../responsive/responsive.service";
import { Router } from "@angular/router";

@Injectable({
  providedIn: 'root'
})
export class ContactService {

  constructor(private _httpClient: HttpClient, private uploadManagerService: UploadManagerService) { }


  sendFormulaireContact(body: any): any {
    return this._httpClient.post(`${environment.host}${environment.apis.upload.formulaireContact}`, body).pipe(
      map((response: any) => {
        this.uploadManagerService.uploadError$.next(null);
        return response;
      })
    );
  }

  private handleError(operation: string) {
    return (err: any) => {
      const errMsg = `error in ${operation}()`;
      throw (errMsg);
    };
  }

}

/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { ViewportScroller } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Transfer } from '@flowjs/ngx-flow/lib/transfer';
import { BehaviorSubject, Observable } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { UploadInfosModel } from 'src/app/models';
import { TokenModel } from 'src/app/models/token.model';
import { environment } from 'src/environments/environment';
import { LoginService } from '../login/login.service';
import { UploadManagerService } from '../upload-manager/upload-manager.service';

@Injectable({
  providedIn: 'root'
})
export class UploadService {

  public langueCourriels = new BehaviorSubject('langueCourriels');
  public checkValidation = new BehaviorSubject(false);

  constructor(private _httpClient: HttpClient,
    private uploadManagerService: UploadManagerService,
    private loginService: LoginService,
    private activatedRoute: ActivatedRoute,
    private viewportScroller: ViewportScroller,) { }


  scroll() {
    this.activatedRoute.fragment.subscribe((fragment: string) => {
      if (fragment) {
        this.scrollToAnchor(fragment)
      } else {
        this.scrollToTop();
      }
    });
  }

  scrollToTop() {
    this.viewportScroller.scrollToPosition([0, 0])

  }

  scrollToAnchor(elementId: string): void {
    this.viewportScroller.scrollToAnchor(elementId);
  }





  sendTree(body: any): any {
    const trMapping = this._mappingTree(body.transfers);
    const treeBody = {
      confirmedSenderId: '',
      senderEmail: body.senderMail,
      recipientEmails: body.emails,
      password: body.password,
      message: body.message,
      subject: body.subject,
      rootFiles: trMapping.files,
      rootDirs: trMapping.dirs,
      publicLink: body.publicLink,
      expireDelay: body.expiryDays,
      senderId: body.senderId,
      senderToken: body.senderToken,
      language: body.langueCourriels,
      zipPassword: body.zipPassword,

    };
    return this._httpClient.post(`${environment.host}${environment.apis.upload.tree}`, treeBody).pipe(
      map((response: any) => {
        this.uploadManagerService.uploadError$.next(null);
        return response;
      }),
      catchError(this.handleError('sendTree'))
    );
  }

  validateMail(mail: any): Observable<any> {
    return this._httpClient.post(`${environment.host}${environment.apis.upload.validateMail}`, mail).pipe(
      map((response: any) => {
        return response;
      }),
      catchError(this.handleError('validateMail'))
    );
  }

  setCheckValidation(checkValidation) {
    this.checkValidation.next(checkValidation);
  }

  setLangueCourriels(langueCourriels) {
    this.langueCourriels.next(langueCourriels);
  }

  allowedSenderMail(mail: any): Observable<any> {
    return this._httpClient.post(`${environment.host}${environment.apis.upload.allowedSenderMail}`, mail).pipe(
      catchError(this.handleError('senderMailNotAllowed'))
    );
  }


  validateCode(body: any): any {
    const trMapping = this._mappingTree(body.transfers);
    const treeBody = {
      confirmedSenderId: '',
      senderEmail: body.senderMail,
      recipientEmails: body.emails,
      password: body.password,
      message: body.message,
      subject: body.subject,
      rootFiles: trMapping.files,
      rootDirs: trMapping.dirs,
      publicLink: body.publicLink,
      expireDelay: body.expiryDays,
      language: body.langueCourriels,
      zipPassword: body.zipPassword,

    };
    return this._httpClient.post(
      `${environment.host}${environment.apis.upload.confirmationCode}?code=${body.code}&senderMail=${body.senderMail}`,
      treeBody
    ).pipe(map((response: UploadInfosModel) => {
      this.uploadManagerService.uploadError$.next(null);
      this.loginService.setLogin({
        senderMail: body.senderMail,
        senderToken: response.senderToken
      });
      this.uploadManagerService.uploadInfos.next({
        canUpload: response.canUpload,
        enclosureId: response.enclosureId,
        expireDate: response.expireDate,
        senderId: response.senderId,
        senderToken: response.senderToken
      });
      return response;
    }),
      catchError(this.handleError('validateCode'))
    );
  }

  rate(body: any): any {
    return this._httpClient.post(`${environment.host}${environment.apis.upload.rate}`, {
      plis: body.plis,
      mailAdress: body.mail,
      message: body.message,
      satisfaction: body.satisfaction,
      token: body.token
    }).pipe(map(response => {
      this.uploadManagerService.uploadError$.next(null);
      return response;
    }),
      catchError(this.handleError('rate'))
    );
  }

  private _mappingTree(transfers: Array<any>) {
    let data = { files: [], dirs: [], totalSize: 0 };
    transfers = transfers.map((transfer: Transfer) => ({
      name: transfer.name,
      path:
        transfer.flowFile.relativePath.split('/').length === 1
          ? ''
          : transfer.flowFile.relativePath.replace(`/${transfer.flowFile.name}`, ''),
      fid: transfer.id,
      size: transfer.size
    }));

    for (let transfer of transfers) {
      this._rec(data, transfer);
    }
    return data;
  }

  private _rec(data: any, file: any) {
    if (file.path.length === 0) {
      data.files.push({ name: file.name, size: file.size, fid: file.fid });
    } else {
      const pathArray = file.path.split('/');
      const parent = pathArray[0];
      file.path = pathArray.slice(1).join('/');
      if (this._dirIndex(data.dirs, parent) !== -1) {
        data.dirs[this._dirIndex(data.dirs, parent)].totalSize =
          data.dirs[this._dirIndex(data.dirs, parent)].totalSize + file.size;
        this._rec(data.dirs[this._dirIndex(data.dirs, parent)], file);
      } else {
        const index =
          data.dirs.push({
            name: parent,
            files: [],
            dirs: [],
            totalSize: file.size
          }) - 1;
        this._rec(data.dirs[index], file);
      }
    }
  }

  private _dirIndex(tab: Array<any>, name: string): number {
    return tab.findIndex(dir => dir.name == name || dir.name == name + '/');
  }

  private handleError(operation: string) {
    return (err: any) => {
      const errMsg = `error in ${operation}()`;
      if (err instanceof HttpErrorResponse) {
        this.uploadManagerService.uploadError$.next({ statusCode: err.status, ...(err.message && !err.error.type) ? { message: err.message } : { message: err.error.type }, ...err.error.codeTryCount ? { codeTryCount: err.error.codeTryCount } : {} });
      }
      throw (errMsg);
    };
  }
}

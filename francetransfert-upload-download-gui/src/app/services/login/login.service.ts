/*
  * Copyright (c) Ministère de la Culture (2022)
  *
  * SPDX-License-Identifier: MIT
  * License-Filename: LICENSE.txt
  */

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';
import { BehaviorSubject, catchError, map, take } from 'rxjs';
import { TokenModel } from 'src/app/models/token.model';
import { environment } from 'src/environments/environment';
import { ConfigService } from '../config/config.service';

@Injectable({
  providedIn: 'root'
})
export class LoginService {

  tokenInfo: BehaviorSubject<TokenModel> = new BehaviorSubject<any>(null);
  loggedIn$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  connectCheck: BehaviorSubject<boolean> = new BehaviorSubject<any>(false);
  public currentTabIndex = 1;  //default tab index is 1

  constructor(private configService: ConfigService, private oauthService: OAuthService, private _httpClient: HttpClient) {

    this.configService.isAgentConnect.subscribe(x => {
      if (x && x == true) {
        const configInfo = this.configService.configInfo.getValue();
        const authCodeFlowConfig: AuthConfig = {
          issuer: configInfo.issuerUrl,
          redirectUri: window.location.origin + '/upload',
          clientId: configInfo.clientId,
          responseType: 'code',
          oidc: true,
          scope: 'openid profile email offline_access',
          strictDiscoveryDocumentValidation: true,
          //showDebugInformation: !environment.production,
          postLogoutRedirectUri: window.location.origin + '/upload',
          //sessionChecksEnabled: true,
        };

        this.oauthService.configure(authCodeFlowConfig);
        this.oauthService.events.subscribe(event => {
          if (event.type == "token_refreshed") {
            const claim: any = this.oauthService.getIdentityClaims();
            this.tokenInfo.next({
              senderMail: claim.email,
              senderToken: "AgentConnect",
              fromSso: true
            });
          } else if (event.type.indexOf("error") > 0) {
            this.logout();
          }
        });
        this.oauthService.loadDiscoveryDocumentAndTryLogin().then(x => {

          const claim: any = this.oauthService.getIdentityClaims();
          if (claim && claim.email && this.oauthService.hasValidAccessToken()) {
            this.oauthService.setupAutomaticSilentRefresh();
            this.connectCheck.next(true);
            this.tokenInfo.next({
              senderMail: claim.email,
              senderToken: "AgentConnect",
              fromSso: true
            });
            this.loggedIn$.next(true);
          } else {
            this.connectCheck.next(false);
            this.tokenInfo.next(null);
            this.loggedIn$.next(false);
          }
        });
      }
    })

  }

  getSsoToken() {
    return this.oauthService.getAccessToken();
  }

  isSso() {
    if (this.tokenInfo.getValue() && this.tokenInfo.getValue().fromSso) {
      return true;
    }
    return false;
  }

  login() {
    //this.oauthService.initImplicitFlow(null, { acr_values: 'eidas1' });
    this.oauthService.initCodeFlow(null, { acr_values: 'eidas1' });
  }


  logout(): any {
    this._httpClient.post(
      `${environment.host}${environment.apis.logout}`,
      {
        senderMail: this.tokenInfo.getValue().senderMail,
        senderToken: this.tokenInfo.getValue().senderToken
      }
    ).pipe(take(1)).subscribe(x => {

    });
    this.connectCheck.next(false);
    this.tokenInfo.next(null);
    this.loggedIn$.next(false);
    this.oauthService.logOut();
  }

  validateCode(body: any, currentLanguage: any): any {
    return this._httpClient.get(
      `${environment.host}${environment.apis.upload.validateCode}?code=${body.code}&senderMail=${body.senderMail}&currentLanguage=${currentLanguage}`
    ).pipe(map((response: TokenModel) => {
      this.tokenInfo.next({
        senderMail: response.senderMail,
        senderToken: response.senderToken
      });
      this.loggedIn$.next(true);
      return response;
    }),
      catchError(this.handleError('validateCode'))
    );
  }

  setLogin(loginData) {
    if (this.connectCheck.getValue() == true) {
      this.tokenInfo.next({
        senderMail: loginData.senderMail,
        senderToken: loginData.senderToken,
        fromSso: false
      });
      this.loggedIn$.next(true);
    }
  }


  generateCode(email: any, currentLanguage: any): any {
    return this._httpClient.get(
      `${environment.host}${environment.apis.upload.generateCode}?senderMail=${email}&currentLanguage=${currentLanguage}`
    ).pipe(map((response: TokenModel) => {
      this.tokenInfo.next(null);
      return response;
    }),
      catchError(this.handleError('generateCode'))
    );
  }

  isLoggedIn(): boolean {
    if (this.tokenInfo.getValue() && this.tokenInfo.getValue().senderMail) {
      return true;
    }
    return false;
  }

  getEmail(): string {
    if (this.isLoggedIn()) {
      return this.tokenInfo.getValue().senderMail;
    }
    return "";
  }

  private handleError(operation: string) {
    return (err: any) => {
      throw (err);
    };
  }



}

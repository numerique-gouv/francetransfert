/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { HttpClient } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';
import { catchError, map, take } from 'rxjs';
import { TokenModel } from 'src/app/models/token.model';
import { environment } from 'src/environments/environment';
import { ConfigService } from '../config/config.service';
import { v4 as uuidv4 } from "uuid";

@Injectable({
  providedIn: 'root'
})
export class LoginService {
  private configService = inject(ConfigService);
  private oauthService = inject(OAuthService);
  private _httpClient = inject(HttpClient);


  readonly tokenInfo = signal<TokenModel | null>(null);
  readonly isLoggedIn = computed(() => !!this.tokenInfo()?.senderMail);
  readonly senderMail = computed(() => this.tokenInfo()?.senderMail ?? '');
  readonly connectCheck = signal(false);
  readonly isAgent = signal(false);
  public currentTabIndex = 1;  //default tab index is 1

  constructor() {
    effect(() => {
      if (this.configService.isAgentConnect()) {
        untracked(() => this.setupAgentConnect());
      }
    });
  }

  private setupAgentConnect() {
    const configInfo = this.configService.configInfo();
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
        this.tokenInfo.set({
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
        this.connectCheck.set(true);
        this.tokenInfo.set({
          senderMail: claim.email,
          senderToken: "AgentConnect",
          fromSso: true
        });
      } else {
        this.connectCheck.set(false);
        this.tokenInfo.set(null);
      }
    });
  }

  getSsoToken() {
    return this.oauthService.getAccessToken();
  }

  isSso() {
    return !!this.tokenInfo()?.fromSso;
  }

  login() {
    //this.oauthService.initImplicitFlow(null, { acr_values: 'eidas1' });
    this.oauthService.initCodeFlow(null, { acr_values: 'eidas1' });
  }


  logout(): any {
    this._httpClient.post(
      `${environment.host}${environment.apis.logout}`,
      {
        senderMail: this.tokenInfo()?.senderMail,
        senderToken: this.tokenInfo()?.senderToken
      }
    ).pipe(take(1)).subscribe(x => {

    });
    if (this.isSso()) {
      this.connectCheck.set(false);
      this.tokenInfo.set(null);
      this.oauthService.logOut();
    }
    this.connectCheck.set(false);
    this.tokenInfo.set(null);

  }

  validateCode(body: any, currentLanguage: any): any {
    return this._httpClient.get(
      `${environment.host}${environment.apis.upload.validateCode}?code=${body.code}&senderMail=${body.senderMail}&currentLanguage=${currentLanguage}`
    ).pipe(map((response: TokenModel) => {
      this.tokenInfo.set({
        senderMail: response.senderMail,
        senderToken: response.senderToken
      });
      return response;
    }),
      catchError(this.handleError('validateCode'))
    );
  }

  setLogin(loginData) {
    if (this.connectCheck()) {
      this.tokenInfo.set({
        senderMail: loginData.senderMail,
        senderToken: loginData.senderToken,
        fromSso: false
      });
    }
  }


  generateCode(email: any, currentLanguage: any): any {
    return this._httpClient.get(
      `${environment.host}${environment.apis.upload.generateCode}?senderMail=${email}&currentLanguage=${currentLanguage}`
    ).pipe(map((response: TokenModel) => {
      this.tokenInfo.set(null);
      return response;
    }),
      catchError(this.handleError('generateCode'))
    );
  }

  private handleError(operation: string) {
    return (err: any) => {
      throw (err);
    };
  }



}

/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const oauthService = inject(OAuthService);

  if (oauthService.hasValidAccessToken() && oauthService.getAccessToken()) {
    const token = oauthService.getAccessToken();
    const modifiedReq = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${token}`),
    });
    return next(modifiedReq).pipe(
      catchError(err => {
        if (err.status === 401) {
          oauthService.logOut(false);
          return of(err);
        }
        throw err;
      })
    );
  } else if (oauthService.hasValidAccessToken() && !oauthService.getAccessToken()) {
    oauthService.logOut(false);
  }
  return next(req);
};

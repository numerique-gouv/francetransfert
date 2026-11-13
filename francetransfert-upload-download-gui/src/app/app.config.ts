/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { APP_BASE_HREF, PlatformLocation } from '@angular/common';
import { provideHttpClient, withInterceptors, withXhr } from '@angular/common/http';
import { ApplicationConfig, inject, isDevMode, provideAppInitializer, provideBrowserGlobalErrorListeners, provideCheckNoChangesConfig } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter, withInMemoryScrolling, withRouterConfig } from '@angular/router';
import { provideServiceWorker } from '@angular/service-worker';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { provideOAuthClient } from 'angular-oauth2-oidc';

import { routes } from './app.routes';
import { PwaService } from './services';
import { coreProviders } from './core.providers';
import { authInterceptor, correlationIdInterceptor } from './shared/interceptors';
import { environment } from '../environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    ...(isDevMode() ? [provideCheckNoChangesConfig({ exhaustive: true, interval: 1000 })] : []),
    provideRouter(
      routes,
      withInMemoryScrolling({ anchorScrolling: 'enabled' }),
      withRouterConfig({ paramsInheritanceStrategy: 'emptyOnly' })
    ),
    provideHttpClient(withXhr(), withInterceptors([correlationIdInterceptor, authInterceptor])),
    provideAnimationsAsync(),
    provideServiceWorker('ngsw-worker.js', {
      enabled: environment.production,
      // Register the ServiceWorker as soon as the app is stable or after 30 seconds.
      registrationStrategy: 'registerWhenStable:30000'
    }),
    provideTranslateService({
      loader: provideTranslateHttpLoader({
        prefix: '/assets/i18n/',
        suffix: '.json',
        useHttpBackend: true,
        failOnError: true
      })
    }),
    provideOAuthClient(),
    ...coreProviders,
    provideAppInitializer(() => inject(PwaService).initPwaPrompt()),
    {
      provide: APP_BASE_HREF,
      useFactory: (s: PlatformLocation) => s.getBaseHrefFromDOM(),
      deps: [PlatformLocation]
    },
  ]
};

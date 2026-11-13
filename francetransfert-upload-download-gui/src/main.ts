/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { registerLocaleData } from '@angular/common';
import localeEn from '@angular/common/locales/en';
import localeEnExtra from '@angular/common/locales/extra/en';
import localeEs from '@angular/common/locales/es';
import localeEsExtra from '@angular/common/locales/extra/es';
import localeFr from '@angular/common/locales/fr';
import localeFrExtra from '@angular/common/locales/extra/fr';
import { bootstrapApplication } from '@angular/platform-browser';

import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';

registerLocaleData(localeEn, 'en', localeEnExtra);
registerLocaleData(localeEs, 'es', localeEsExtra);
registerLocaleData(localeFr, 'fr', localeFrExtra);

bootstrapApplication(AppComponent, appConfig)
  .catch(err => console.error(err));

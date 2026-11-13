/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

// Providers applied to every TestBed (angular.json > test > options.providersFile).
// They reuse the coreProviders shared with app.config.ts and stub what has network side effects.
import { APP_BASE_HREF } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, EnvironmentProviders, Provider, provideZonelessChangeDetection } from '@angular/core';
import { MatBottomSheetRef, MAT_BOTTOM_SHEET_DATA } from '@angular/material/bottom-sheet';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MAT_SNACK_BAR_DATA, MatSnackBarRef } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { SwUpdate } from '@angular/service-worker';
import { provideTranslateService } from '@ngx-translate/core';
import { provideOAuthClient } from 'angular-oauth2-oidc';
import { of, Subject } from 'rxjs';

import { coreProviders } from './app/core.providers';

// Target for the navigations some components trigger on init (/upload, /error...)
@Component({ template: '' })
class BlankRouteComponent { }

const swUpdateStub: Partial<SwUpdate> = {
  isEnabled: false,
  versionUpdates: new Subject<any>(),
  unrecoverable: new Subject<any>(),
  checkForUpdate: () => Promise.resolve(false),
  activateUpdate: () => Promise.resolve(false),
};

const providers: (Provider | EnvironmentProviders)[] = [
  provideZonelessChangeDetection(),
  provideHttpClient(),
  provideHttpClientTesting(),
  provideRouter([{ path: '**', component: BlankRouteComponent }]),
  { provide: APP_BASE_HREF, useValue: '/' },
  provideNoopAnimations(),
  provideTranslateService(),
  provideOAuthClient(),
  { provide: SwUpdate, useValue: swUpdateStub },
  ...coreProviders,
  // Dialog / bottom-sheet / snack-bar content components
  { provide: MAT_DIALOG_DATA, useValue: {} },
  { provide: MatDialogRef, useValue: { close: () => undefined, afterClosed: () => of(undefined) } },
  { provide: MAT_BOTTOM_SHEET_DATA, useValue: {} },
  { provide: MatBottomSheetRef, useValue: { dismiss: () => undefined } },
  { provide: MAT_SNACK_BAR_DATA, useValue: {} },
  { provide: MatSnackBarRef, useValue: { dismiss: () => undefined } },
];

export default providers;

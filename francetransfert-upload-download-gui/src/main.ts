/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { AppModule } from './app/app.module';
import { environment } from './environments/environment';
import { isIOS } from './app/shared/is-ios';

if (environment.production) {
  enableProdMode();
}


async function unregisterServiceWorkerOnIOS(): Promise<void> {
  if (!isIOS() || !('serviceWorker' in navigator)) {
    return;
  }

  const registrations = await navigator.serviceWorker.getRegistrations();
  if (registrations.length > 0) {
    await Promise.all(registrations.map(registration => registration.unregister()));
  }

  if ('caches' in window) {
    const keys = await caches.keys();
    if (keys.length > 0) {
      await Promise.all(keys.map(key => caches.delete(key)));
    }
  }
}

unregisterServiceWorkerOnIOS()
  .catch(err => console.error('Failed to unregister service worker on iOS', err))
  .finally(() => {
    platformBrowserDynamic().bootstrapModule(AppModule)
      .catch(err => console.error(err));
  });

/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { Platform } from '@angular/cdk/platform';
import { Injectable, inject, signal } from '@angular/core';
import { MatBottomSheet } from '@angular/material/bottom-sheet';
import { MatDialog } from '@angular/material/dialog';
import { SwUpdate, VersionReadyEvent } from '@angular/service-worker';
import { interval, timer } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { PwaPromptInstallComponent } from 'src/app/components/pwa/pwa-prompt-install/pwa-prompt-install.component';
import { PwaPromptUpdateComponent } from 'src/app/components/pwa/pwa-prompt-update/pwa-prompt-update.component';

@Injectable({
  providedIn: 'root'
})
export class PwaService {
  private swUpdate = inject(SwUpdate);
  private _dialog = inject(MatDialog);
  private bottomSheet = inject(MatBottomSheet);
  private platform = inject(Platform);


  private promptEvent: any;
  readonly appInstalled = signal(false);
  readonly appUpdateAvailable = signal(false);

  constructor() {
    const swUpdate = this.swUpdate;


    if (swUpdate.isEnabled) {
      interval(6 * 60 * 60).subscribe(() => swUpdate.checkForUpdate()
        .then(() => {
          if (document.location.hostname === 'francetransfert.culture.gouv.fr') {
            window.location.href = "https://francetransfert.numerique.gouv.fr";
          }
          console.log('checking for updates');
        }));
    }

  }

  checkForUpdates() {
    this.swUpdate.versionUpdates.pipe(
      filter((evt): evt is VersionReadyEvent => evt.type === 'VERSION_READY')
    ).subscribe(() => {
      this.appUpdateAvailable.set(true);
      this.promptUserUpdate();
    });
  }

  promptUserUpdate(): void {
    console.log('updating to new version');
    this.swUpdate.activateUpdate().then(() => {
      const dialogRef = this._dialog.open(PwaPromptUpdateComponent, {
        width: '500px'
      });
      dialogRef.afterClosed().subscribe(result => {
        if (result) {
          this.appUpdateAvailable.set(false);
          window.location.reload();
        }
      });
    });
  }

  promptUserInstall() {
    this.initPwaPrompt();
  }

  public initPwaPrompt() {
    if (this.platform.ANDROID) {
      window.addEventListener('beforeinstallprompt', (event: any) => {
        if (event.origin && event.origin !== window.location.origin) {
          return;
        }
        event.preventDefault();
        this.promptEvent = event;
        this.openPromptComponent('android');
      });
    }
    if (this.platform.IOS) {
      const isInStandaloneMode = ('standalone' in window.navigator) && (window.navigator['standalone']);
      if (!isInStandaloneMode) {
        this.openPromptComponent('ios');
      }
    }
  }

  private openPromptComponent(mobileType: 'ios' | 'android') {
    timer(3000)
      .pipe(take(1))
      .subscribe(() => this.bottomSheet.open(PwaPromptInstallComponent, { data: { mobileType, promptEvent: this.promptEvent } }));
  }
}

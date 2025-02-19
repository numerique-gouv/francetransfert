/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { Platform } from '@angular/cdk/platform';
import { Injectable } from '@angular/core';
import { MatBottomSheet } from '@angular/material/bottom-sheet';
import { MatLegacyDialog as MatDialog } from '@angular/material/legacy-dialog';
import { SwUpdate } from '@angular/service-worker';
import { interval, BehaviorSubject, timer } from 'rxjs';
import { take } from 'rxjs/operators';
import { PwaPromptInstallComponent, PwaPromptUpdateComponent } from 'src/app/components';

@Injectable({
  providedIn: 'root'
})
export class PwaService {

  private promptEvent: any;
  appInstalled$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  appUpdateAvailable$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  constructor(private swUpdate: SwUpdate,
    private _dialog: MatDialog,
    private bottomSheet: MatBottomSheet,
    private platform: Platform) {

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
    this.swUpdate.available.subscribe(event => {
      this.appUpdateAvailable$.next(true);
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
          this.appUpdateAvailable$.next(false);
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

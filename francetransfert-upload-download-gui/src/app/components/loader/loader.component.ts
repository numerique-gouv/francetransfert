/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { Component, effect, inject, output, signal, untracked } from '@angular/core';
import { ProgressSpinnerMode, MatProgressSpinner } from '@angular/material/progress-spinner';
import { UploadState } from '@flowjs/ngx-flow';
import { FileManagerService } from 'src/app/services';
import { LoginService } from 'src/app/services/login/login.service';
import { MatCard, MatCardContent, MatCardActions } from '@angular/material/card';
import { MatButton } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';


@Component({
    selector: 'ft-loader',
    templateUrl: './loader.component.html',
    styleUrls: ['./loader.component.scss'],
    imports: [MatCard, MatCardContent, MatProgressSpinner, MatCardActions, MatButton, TranslatePipe]
})
export class LoaderComponent {
  private fileManagerService = inject(FileManagerService);
  private loginService = inject(LoginService);


  mode: ProgressSpinnerMode = 'determinate';
  readonly progressValue = signal<number>(0);
  readonly transferCancelled = output<boolean>();
  readonly transferFinished = output<boolean>();
  readonly transferFailed = output<boolean>();
  private cancelled = false;

  constructor() {
    effect(() => {
      const uploadState = this.fileManagerService.uploadProgress();
      untracked(() => this.onUploadProgress(uploadState));
    });
  }

  private onUploadProgress(uploadState: UploadState | null) {
    if (!uploadState || this.cancelled) {
      return; 
    }
    if (this.haveChunkError(uploadState)) {
      this.cancelTransfer();
      console.log('Transfert Error');
    }
    this.progressValue.set(Math.round(uploadState.totalProgress * 100));
    if (this.progressValue() >= 100) {
      if (this.isFinish(uploadState) && !this.haveChunkError(uploadState)) {
        this.transferFinished.emit(true);
      } else if (this.haveChunkError(uploadState)) {
        this.cancelTransfer();
        console.log('Transfert Error');
      }
    }
  }

  cancelTransfer() {
    this.cancelled = true;
    this.transferFailed.emit(true);
  }


  isLoggedIn(): boolean {
    return this.loginService.isLoggedIn();
  }

  haveChunkError(uploadState: UploadState): boolean {
    if (uploadState && uploadState.transfers) {
      for (let transfer of uploadState.transfers) {
        if (transfer.error && !transfer.success) {
          return true;
        }
      }
      return false;
    }
    return false;
  }

  isFinish(uploadState: UploadState): boolean {
    return uploadState.transfers.every(x => x.complete == true);
  }

}

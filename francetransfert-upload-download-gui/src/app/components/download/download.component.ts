/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { FlowConfig, Transfer } from '@flowjs/ngx-flow';
import { take } from 'rxjs/operators';
import { FTTransferModel } from 'src/app/models';
import { DownloadManagerService, DownloadService, ResponsiveService, UploadManagerService } from 'src/app/services';
import { FLOW_CONFIG } from 'src/app/shared/config/flow-config';

import { SatisfactionMessageComponent } from "../satisfaction-message/satisfaction-message.component";
import { MatSnackBar } from "@angular/material/snack-bar";
import { LoginService } from 'src/app/services/login/login.service';
import { DownloadErrorComponent } from './download-error/download-error.component';
import { NgStyle } from '@angular/common';
import { CheckValidationCodeComponent } from '../check-validation-code/check-validation-code.component';
import { ListElementsComponent } from '../list-elements/list-elements.component';
import { DownloadElementsComponent } from './download-elements/download-elements.component';
import { EndMessageComponent } from '../end-message/end-message.component';
import { SatisfactionCheckComponent } from '../satisfaction-check/satisfaction-check.component';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

@Component({
    selector: 'ft-download',
    templateUrl: './download.component.html',
    styleUrls: ['./download.component.scss'],
    imports: [FlowConfig, DownloadErrorComponent, NgStyle, CheckValidationCodeComponent, ListElementsComponent, DownloadElementsComponent, EndMessageComponent, SatisfactionCheckComponent, MatProgressSpinner]
})
export class DownloadComponent implements OnInit {
  private _downloadService = inject(DownloadService);
  private responsiveService = inject(ResponsiveService);
  private _activatedRoute = inject(ActivatedRoute);
  private uploadManagerService = inject(UploadManagerService);
  private downloadManagerService = inject(DownloadManagerService);
  private loginService = inject(LoginService);
  private _router = inject(Router);
  private titleService = inject(Title);
  private _snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly downloadValidated = signal<boolean>(false);
  readonly downloadStarted = signal<boolean>(false);
  readonly usingPublicLink = signal<boolean>(false);
  readonly transfers = signal<Array<any>>([]);
  readonly downloadInfos = signal<any>(undefined);
  emails: Array<string>;
  password: string;
  passwordError: boolean;
  withPassword: boolean;
  params: Array<{ string: string }>;
  flowConfig: any;
  readonly loading = signal<boolean>(true);
  readonly screenWidth = this.responsiveService.screenWidth;

  ngOnInit(): void {
    this.titleService.setTitle('France transfert - Téléchargement');
    this.uploadManagerService.uploadError.set(null);
    this.downloadManagerService.downloadError.set(null);
    this.flowConfig = FLOW_CONFIG;
    this._activatedRoute.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params: Array<{ string: string }>) => {
      this.params = params;
      this.loading.set(true);
      if (this.params['enclosure'] && this.params['recipient'] && this.params['token']) {
        this._downloadService
          .existsEnclosure(params)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: (existsEnclosure) => {
              if (existsEnclosure) {
                this.downloadInfos.set({});
              }
            },
            error: () => { this.loading.set(false) },
            complete: () => { this.loading.set(false); }
          });
      } else if (this.loginService.isLoggedIn() && this.params['recipient'] == null && this.params['enclosure'] && this.params['token'] == null && this.params['enclosure'] !== '') {
        this._downloadService
          .getDownloadInfosConnect(this.params['enclosure'],
            this.loginService.tokenInfo().senderToken,
            this.loginService.tokenInfo().senderMail
          ).pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: (downloadInfos) => {
              this.downloadInfos.set(downloadInfos);
              this.addTransfers(downloadInfos);
              this.downloadValidated.set(true);
            },
            error: () => { this.loading.set(false) },
            complete: () => { this.loading.set(false); }
          });
      } else {
        if (this._router.url.includes('download/download-info-public')) {
          if (this.params['enclosure'] && this.params['enclosure'] !== '') {
            this._downloadService
              .existsPublicEnclosure(params)
              .pipe(takeUntilDestroyed(this.destroyRef))
              .subscribe({
                next: (existsPublicEnclosure) => {
                  if (existsPublicEnclosure) {
                    this.downloadInfos.set({});
                  }
                },
                error: () => { this.loading.set(false) },
                complete: () => { this.loading.set(false); }
              });
            this.usingPublicLink.set(true);
          } else {
            this._router.navigateByUrl('/upload');
          }
        } else {
          this._router.navigateByUrl('/upload');
        }
      }
    });
  }

  download(): void {
    if (this.usingPublicLink()) {
      this._downloadService
        .getDownloadUrlPublic(this.params, this.password)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(result => {
          if (result.type && result.type === 'WRONG_PASSWORD') {
            this.passwordError = true;
          } else {
            window.location.assign(result.lienTelechargement);
            this.downloadStarted.set(true);
          }
        });
    } else {
      this._downloadService
        .getDownloadUrl(this.params, this.downloadInfos().withPassword, this.password)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(result => {
          if (result.type && result.type === 'WRONG_PASSWORD') {
            this.passwordError = true;
          } else {
            window.location.assign(result.lienTelechargement);
            this.downloadStarted.set(true);
          }
        });
    }

  }

  onDowloadStarted(event) {
    if (event) {
      this.download();
    }
  }

  onDownloadValidated(event) {
    this.password = event;
    let recipient;
    if (!this.usingPublicLink()) {
      recipient = this.params['recipient'] ? this.params['recipient'] : this.loginService.tokenInfo()?.senderMail;
    }
    this._downloadService.validatePassword({ enclosureId: this.params['enclosure'], password: this.password, recipientId: recipient }).pipe(take(1))
      .subscribe((response) => {
        if (response.valid) {
          this.loading.set(true);
          let params = {
            enclosure: this.params['enclosure'],
            recipient: this.params['recipient'],
            token: this.params['token'],
            password: this.password,
          };
          if (this.params['enclosure'] && this.params['recipient'] && this.params['token']) {
            this._downloadService
              .getDownloadInfos(params)
              .pipe(takeUntilDestroyed(this.destroyRef))
              .subscribe({
                next: (downloadInfos) => {
                  this.setPliInfo(downloadInfos);
                  this.downloadValidated.set(true);
                  this.downloadManagerService.downloadError.set(null);
                },
                error: () => { this.loading.set(false) },
                complete: () => { this.loading.set(false); }
              });
          } else if (this._router.url.includes('download/download-info-public')) {
            if (this.params['enclosure'] && this.params['enclosure'] !== '') {
              this._downloadService
                .getDownloadInfosPublic(params)
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe({
                  next: (downloadInfosPublic) => {
                    this.setPliInfo(downloadInfosPublic);
                    this.downloadValidated.set(true);
                    this.downloadManagerService.downloadError.set(null);
                  },
                  error: () => { this.loading.set(false) },
                  complete: () => { this.loading.set(false); }
                });
              this.usingPublicLink.set(true);
            } else {
              this._router.navigateByUrl('/upload');
            }
          }
        }
      },
        () => {
          
        });
  }

  private setPliInfo(downloadInfos: Object) {
    this.downloadInfos.set(downloadInfos);
    this.addTransfers(downloadInfos);
  }

  private addTransfers(downloadInfos: any) {
    this.transfers.update(transfers => [
      ...transfers,
      ...downloadInfos.rootFiles.map(file => ({ ...file, folder: false } as FTTransferModel<Transfer>)),
      ...downloadInfos.rootDirs.map(file => ({ ...file, size: file.totalSize, folder: true } as FTTransferModel<Transfer>)),
    ]);
  }

  onSatisfactionCheckDone(event) {
    if (event) {
      this._downloadService.rate({
        plis: this.params['enclosure'], mail: this.downloadInfos().recipientMail, message: event.message, satisfaction: event.satisfaction,
        ...this.loginService.tokenInfo()?.senderToken ? { token: this.loginService.tokenInfo()?.senderToken } : { token: this.params['token'] },
      }).pipe(take(1))
        .subscribe((result) => {
          if (result) {
            this.openSnackBar(4000);
          }
          this._router.navigate(['/upload']);
        });
    }
  }
  openSnackBar(duration: number) {
    this._snackBar.openFromComponent(SatisfactionMessageComponent, {
      duration: duration
    });
  }

  styleObject(): Object {
    if (this.screenWidth() === 'lg') {
      return { '-webkit-flex-direction': 'row' }
    }
    if (this.screenWidth() === 'md') {
      if (!this.downloadValidated() && !this.downloadStarted()) {
        return { '-webkit-flex-direction': 'column-reverse' }
      } else {
        return { '-webkit-flex-direction': 'row' }
      }
    }
    if (this.screenWidth() === 'sm') {
      if (this.downloadValidated() && this.downloadStarted()) {
        return { '-webkit-flex-direction': 'column' }
      } else {
        return { '-webkit-flex-direction': 'column-reverse' }
      }
    }
    return {}
  }

}

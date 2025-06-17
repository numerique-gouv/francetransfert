/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { FlowDirective, Transfer } from '@flowjs/ngx-flow';
import { Subject } from 'rxjs/internal/Subject';
import { take, takeUntil } from 'rxjs/operators';
import { FTTransferModel } from 'src/app/models';
import { DownloadManagerService, DownloadService, ResponsiveService, UploadManagerService } from 'src/app/services';
import { FLOW_CONFIG } from 'src/app/shared/config/flow-config';
import { Subscription } from "rxjs";
import { SatisfactionMessageComponent } from "../satisfaction-message/satisfaction-message.component";
import { MatLegacySnackBar as MatSnackBar } from "@angular/material/legacy-snack-bar";
import { LoginService } from 'src/app/services/login/login.service';

@Component({
  selector: 'ft-download',
  templateUrl: './download.component.html',
  styleUrls: ['./download.component.scss']
})
export class DownloadComponent implements OnInit, OnDestroy {
  private onDestroy$: Subject<void> = new Subject();
  downloadValidated: boolean = false;
  downloadStarted: boolean = false;
  usingPublicLink: boolean = false;
  responsiveSubscription: Subscription = new Subscription;
  transfers: Array<any> = [];
  downloadInfos: any;
  emails: Array<string>;
  password: string;
  passwordError: boolean;
  withPassword: boolean;
  params: Array<{ string: string }>;
  @ViewChild('flow')
  flow: FlowDirective;
  flowConfig: any;
  loading: boolean = true;
  isMobile: boolean = false;
  screenWidth: string;

  constructor(private _downloadService: DownloadService,
    private responsiveService: ResponsiveService,
    private _activatedRoute: ActivatedRoute,
    private uploadManagerService: UploadManagerService,
    private downloadManagerService: DownloadManagerService,
    private loginService: LoginService,
    private _router: Router,
    private titleService: Title,
    private _snackBar: MatSnackBar) { }

  ngOnInit(): void {
    this.titleService.setTitle('France transfert - Téléchargement');
    this.onResize();
    this.uploadManagerService.uploadError$.next(null);
    this.downloadManagerService.downloadError$.next(null);
    this.flowConfig = FLOW_CONFIG;
    this._activatedRoute.queryParams.pipe(takeUntil(this.onDestroy$)).subscribe((params: Array<{ string: string }>) => {
      this.params = params;
      this.loading = true;
      if (this.params['enclosure'] && this.params['recipient'] && this.params['token']) {
        this._downloadService
          .getDownloadInfos(params)
          .pipe(takeUntil(this.onDestroy$))
          .subscribe({
            next: (downloadInfos) => {
              this.downloadInfos = downloadInfos;
              this.downloadInfos.rootFiles.map(file => {
                this.transfers.push({ ...file, folder: false } as FTTransferModel<Transfer>);
              });
              this.downloadInfos.rootDirs.map(file => {
                this.transfers.push({ ...file, size: file.totalSize, folder: true } as FTTransferModel<Transfer>);
              });
            },
            error: () => { this.loading = false },
            complete: () => { this.loading = false; }
          });
      } else if (this.loginService.isLoggedIn() && this.params['recipient'] == null && this.params['enclosure'] && this.params['token'] == null) {
        this._downloadService
          .getDownloadInfosConnect(this.params['enclosure'],
            this.loginService.tokenInfo.getValue().senderToken,
            this.loginService.tokenInfo.getValue().senderMail
          ).pipe(takeUntil(this.onDestroy$))
          .subscribe({
            next: (downloadInfos) => {
              this.downloadInfos = downloadInfos;
              this.downloadInfos.rootFiles.map(file => {
                this.transfers.push({ ...file, folder: false } as FTTransferModel<Transfer>);
              });
              this.downloadInfos.rootDirs.map(file => {
                this.transfers.push({ ...file, size: file.totalSize, folder: true } as FTTransferModel<Transfer>);
              });
              this.downloadValidated = true;
            },
            error: () => { this.loading = false },
            complete: () => { this.loading = false; }
          });
      } else {
        if (this._router.url.includes('download/download-info-public')) {
          if (this.params['enclosure']) {
            this._downloadService
              .getDownloadInfosPublic(params)
              .pipe(takeUntil(this.onDestroy$))
              .subscribe({
                next: (downloadInfosPublic) => {
                  this.downloadInfos = downloadInfosPublic;
                  this.downloadInfos.rootFiles.map(file => {
                    this.transfers.push({ ...file, folder: false } as FTTransferModel<Transfer>);
                  });
                  this.downloadInfos.rootDirs.map(file => {
                    this.transfers.push({ ...file, size: file.totalSize, folder: true } as FTTransferModel<Transfer>);
                  });
                },
                error: () => { this.loading = false },
                complete: () => { this.loading = false; }
              });
            this.usingPublicLink = true;
          }
        } else {
          this._router.navigateByUrl('/error');
        }
      }
    });
  }

  download(): void {
    if (this.usingPublicLink) {
      this._downloadService
        .getDownloadUrlPublic(this.params, this.password)
        .pipe(takeUntil(this.onDestroy$))
        .subscribe(result => {
          if (result.type && result.type === 'WRONG_PASSWORD') {
            this.passwordError = true;
          } else {
            window.location.assign(result.lienTelechargement);
            this.downloadStarted = true;
          }
        });
    } else {
      this._downloadService
        .getDownloadUrl(this.params, this.downloadInfos.withPassword, this.password)
        .pipe(takeUntil(this.onDestroy$))
        .subscribe(result => {
          if (result.type && result.type === 'WRONG_PASSWORD') {
            this.passwordError = true;
          } else {
            window.location.assign(result.lienTelechargement);
            this.downloadStarted = true;
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
    if (!this.usingPublicLink) {
      recipient = this.params['recipient'] ? this.params['recipient'] : this.loginService.tokenInfo?.getValue()?.senderMail;
    }
    this._downloadService.validatePassword({ enclosureId: this.params['enclosure'], password: this.password, recipientId: recipient }).pipe(take(1))
      .subscribe((response) => {
        if (response.valid) {
          this.downloadValidated = true;
          this.downloadManagerService.downloadError$.next(null);
        }
      },
        (error) => {
          this.downloadManagerService.downloadError$.next(error);
        });
  }

  ngOnDestroy() {
    this.onDestroy$.next();
    this.onDestroy$.complete();
    this.responsiveSubscription.unsubscribe();
  }

  onSatisfactionCheckDone(event) {
    if (event) {
      this._downloadService.rate({ plis: this.params['enclosure'], mail: this.downloadInfos.recipientMail, message: event.message, satisfaction: event.satisfaction }).pipe(take(1))
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

  onResize() {
    this.responsiveSubscription = this.responsiveService.getMobileStatus().subscribe(isMobile => {
      this.isMobile = isMobile;
      this.screenWidth = this.responsiveService.screenWidth;
    });
  }

  styleObject(): Object {
    if (this.screenWidth === 'lg') {
      return { '-webkit-flex-direction': 'row' }
    }
    if (this.screenWidth === 'md') {
      if (!this.downloadValidated && !this.downloadStarted) {
        return { '-webkit-flex-direction': 'column-reverse' }
      } else {
        return { '-webkit-flex-direction': 'row' }
      }
    }
    if (this.screenWidth === 'sm') {
      if (this.downloadValidated && this.downloadStarted) {
        return { '-webkit-flex-direction': 'column' }
      } else {
        return { '-webkit-flex-direction': 'column-reverse' }
      }
    }
    return {}
  }

}

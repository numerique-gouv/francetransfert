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
import { DownloadManagerService, DownloadService, FileEncryptionService, KeyPairService, ResponsiveService, UploadManagerService } from 'src/app/services';
import { FLOW_CONFIG } from 'src/app/shared/config/flow-config';
import { Subscription } from "rxjs";
import { SatisfactionMessageComponent } from "../satisfaction-message/satisfaction-message.component";
import { MatLegacySnackBar as MatSnackBar } from "@angular/material/legacy-snack-bar";
import { LoginService } from 'src/app/services/login/login.service';
import { showSaveFilePicker } from 'native-file-system-adapter';
import * as streamSaver from 'streamsaver';

(streamSaver as any).mitm = '/streamsaver/mitm.html';

type DownloadFileHandle = {
  name: string;
  createWritable: () => Promise<WritableStream<Uint8Array>>;
};

@Component({
  selector: 'ft-download',
  templateUrl: './download.component.html',
  styleUrls: ['./download.component.scss']
})
export class DownloadComponent implements OnInit, OnDestroy {
  private onDestroy$: Subject<void> = new Subject();
  private streamSaverReadyPromise?: Promise<void>;
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
  isDownloading: boolean = false;
  isMobile: boolean = false;
  screenWidth: string;

  constructor(
    private _downloadService: DownloadService,
    private responsiveService: ResponsiveService,
    private _activatedRoute: ActivatedRoute,
    private uploadManagerService: UploadManagerService,
    private downloadManagerService: DownloadManagerService,
    private loginService: LoginService,
    private _router: Router,
    private titleService: Title,
    private _snackBar: MatSnackBar,
    private keyPairService: KeyPairService,
    private fileEncryptionService: FileEncryptionService
  ) { }

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
              void this.decryptAndStorePliKeyIfPresent(this.downloadInfos, false);
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
      } else if (this.loginService.isLoggedIn() && this.params['recipient'] == null && this.params['enclosure'] && this.params['token'] == null && this.params['enclosure'] !== '') {
        this._downloadService
          .getDownloadInfosConnect(this.params['enclosure'],
            this.loginService.tokenInfo.getValue().senderToken,
            this.loginService.tokenInfo.getValue().senderMail
          ).pipe(takeUntil(this.onDestroy$))
          .subscribe({
            next: (downloadInfos) => {
              this.downloadInfos = downloadInfos;
              void this.decryptAndStorePliKeyIfPresent(this.downloadInfos, false);
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
          if (this.params['enclosure'] && this.params['enclosure'] !== '') {
            this._downloadService
              .getDownloadInfosPublic(params)
              .pipe(takeUntil(this.onDestroy$))
              .subscribe({
                next: (downloadInfosPublic) => {
                  this.downloadInfos = downloadInfosPublic;
                  void this.decryptAndStorePliKeyIfPresent(this.downloadInfos, false);
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
    if (this.isDownloading) {
      return;
    }
    this.isDownloading = true;
    if (this.usingPublicLink) {
      this._downloadService
        .getDownloadUrlPublic(this.params, this.password)
        .pipe(takeUntil(this.onDestroy$))
        .subscribe({
          next: result => {
            if (result.type && result.type === 'WRONG_PASSWORD') {
              this.passwordError = true;
              this.isDownloading = false;
            } else {
              this.runDownloadFlow(result.lienTelechargement);
            }
          },
          error: (err) => {
            this.isDownloading = false;
          }
        });
    } else {
      this._downloadService
        .getDownloadUrl(this.params, this.downloadInfos.withPassword, this.password)
        .pipe(takeUntil(this.onDestroy$))
        .subscribe({
          next: result => {
            if (result.type && result.type === 'WRONG_PASSWORD') {
              this.passwordError = true;
              this.isDownloading = false;
            } else {
              this.runDownloadFlow(result.lienTelechargement);
            }
          },
          error: (err) => {
            this.isDownloading = false;
          }
        });
    }
  }

  private runDownloadFlow(presignedUrl: string): void {
    console.log('runDownloadFlow', presignedUrl);
    const pliKey = this.downloadManagerService.pliAesKey.getValue();

    if (pliKey) {
      // Vrai streaming : fetch ReadableStream → TransformStream decrypt
      // → StreamSaver disque
      console.log('decryptAndStreamToFile', presignedUrl, pliKey);
      void this.decryptAndStreamToFile(presignedUrl, pliKey)
        .catch((_err) => {
          this.downloadManagerService.downloadError$.next({
            statusCode: 0,
            message: 'DECRYPTION_FAILED',
          });
        })
        .finally(() => {
          // this.downloadManagerService.clearPliAesKey();
          this.isDownloading = false;
        });
    } else {
      this.downloadFileFromUrl(presignedUrl);
      this.downloadStarted = true;
      this.isDownloading = false;
    }
  }

  private async decryptAndStreamToFile(presignedUrl: string, pliKey: Uint8Array): Promise<void> {
    const filename = this.downloadInfos?.rootFiles?.[0]?.name ?? 'download';
    const fileHandle = await this.openDownloadFileHandle(filename);
    const writableStream = await fileHandle.createWritable();
    const writer = writableStream.getWriter();
    let completed = false;

    try {
      // 2. Fetch streaming directement depuis l'URL pré-signée
      const response = await fetch(presignedUrl);
      if (!response.ok || !response.body) {
        throw new Error(`DOWNLOAD_STREAM_ERROR: HTTP ${response.status}`);
      }
      const encryptedStream = response.body as ReadableStream<Uint8Array>;

      // 3. Pipeline : fetch → decrypt (secretstream pull) → écriture disque
      const decryptTransform = this.fileEncryptionService.createDecryptTransformStream(pliKey);
      const reader = encryptedStream.pipeThrough(decryptTransform).getReader();
      while (true) {
        const { value, done } = await reader.read();
        if (done) { console.log('done'); break; }
        await writer.write(value);
      }
      await writer.close();
      completed = true;
      this.downloadStarted = true;
    } finally {
      if (!completed) {
        await writer.abort();
      }
    }
  }

  private async openDownloadFileHandle(filename: string): Promise<DownloadFileHandle> {
    const baseOptions = {
      suggestedName: filename,
      excludeAcceptAllOption: false,
    } as const;

    if (this.hasNativeSavePickerSupport()) {
      try {
        return await showSaveFilePicker({
          ...baseOptions,
          _preferredMethods: ['native'],
        } as any) as DownloadFileHandle;
      } catch (error) {
        console.error('openDownloadFileHandle error', error);
      }
    }

    await this.ensureStreamSaverServiceWorkerReady();
    const fileStream = streamSaver.createWriteStream(filename) as WritableStream<Uint8Array>;
    return {
      name: filename,
      createWritable: async () => fileStream,
    };
  }

  private hasNativeSavePickerSupport(): boolean {
    return typeof window !== 'undefined' && typeof (window as any).showSaveFilePicker === 'function';
  }

  private ensureStreamSaverServiceWorkerReady(): Promise<void> {
    if (this.streamSaverReadyPromise !== undefined) {
      return this.streamSaverReadyPromise;
    }

    this.streamSaverReadyPromise = (async () => {
      if (typeof window === 'undefined' || !('serviceWorker' in navigator)) {
        return;
      }

      const scope = '/streamsaver/';
      const workerUrl = `${scope}sw.js`;
      let registration = await navigator.serviceWorker.getRegistration(scope);

      if (!registration) {
        registration = await navigator.serviceWorker.register(workerUrl, { scope });
      }

      console.log('StreamSaver service worker registration', registration);

      if (registration.active) {
        console.log('StreamSaver service worker already active');
        return;
      }

      const pendingWorker = registration.installing ?? registration.waiting;
      if (!pendingWorker) {
        console.log('No pending StreamSaver service worker');
        return;
      }

      await new Promise<void>((resolve, reject) => {
        const timeoutId = window.setTimeout(() => {
          pendingWorker.removeEventListener('statechange', onStateChange);
          reject(new Error('STREAMSAVER_SW_ACTIVATION_TIMEOUT'));
        }, 10000);

        const onStateChange = (): void => {
          if (pendingWorker.state === 'activated') {
            window.clearTimeout(timeoutId);
            pendingWorker.removeEventListener('statechange', onStateChange);
            resolve();
          }
        };

        pendingWorker.addEventListener('statechange', onStateChange);
      });
    })().catch((error) => {
      console.error('ensureStreamSaverServiceWorkerReady error', error);
      this.streamSaverReadyPromise = undefined;
      throw error;
    });

    return this.streamSaverReadyPromise;
  }

  private downloadFileFromUrl(url: string): void {
    globalThis.location.assign(url);
  }


  private async decryptAndStorePliKeyIfPresent(downloadInfos: { pliAesKeyEncrypted?: string }, isSender: boolean): Promise<void> {
    this.downloadManagerService.clearPliAesKey();
    const encrypted = downloadInfos?.pliAesKeyEncrypted;
    if (!encrypted) {
      return;
    }
    const pairs = await this.keyPairService.getPocEnrollmentKeyPairs();
    const toTry = isSender
      ? [pairs.sender]
      : [pairs.sender, pairs.recipient1, pairs.recipient2];
    for (const pair of toTry) {
      try {
        const pliKey = await this.fileEncryptionService.unwrapPliKey(
          encrypted, pair.publicKey, pair.privateKey
        );
        this.downloadManagerService.setPliAesKey(pliKey);
        console.log('decryptAndStorePliKeyIfPresent success');
        return;
      } catch (error) {
        // console.error('decryptAndStorePliKeyIfPresent', error);
        continue;
      }
    }
    console.error('decryptAndStorePliKeyIfPresent failed, all attempts failed');
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
          const isSenderFlow = !this.usingPublicLink && !this.params['recipient'];
          void this.decryptAndStorePliKeyIfPresent(
            { pliAesKeyEncrypted: response?.pliAesKeyEncrypted },
            isSenderFlow
          ).then(() => {
            this.downloadValidated = true;
            this.downloadManagerService.downloadError$.next(null);
          }).catch((error) => {
            this.downloadValidated = true;
            this.downloadManagerService.downloadError$.next(null);
          });
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
      this._downloadService.rate({
        plis: this.params['enclosure'], mail: this.downloadInfos.recipientMail, message: event.message, satisfaction: event.satisfaction,
        ...this.loginService.tokenInfo.getValue()?.senderToken ? { token: this.loginService.tokenInfo.getValue()?.senderToken } : { token: this.params['token'] },
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

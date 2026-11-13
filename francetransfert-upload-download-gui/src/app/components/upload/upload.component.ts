/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { AfterViewInit, Component, DestroyRef, ElementRef, OnDestroy, OnInit, effect, inject, viewChild, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { FlowConfig, UploadState } from '@flowjs/ngx-flow';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';
import { DownloadManagerService, FileManagerService, LanguageSelectionService, ResponsiveService, UploadManagerService, UploadService } from 'src/app/services';
import { FLOW_CONFIG } from 'src/app/shared/config/flow-config';
import { MatSnackBar } from "@angular/material/snack-bar";
import { SatisfactionMessageComponent } from "../satisfaction-message/satisfaction-message.component";
import { Router } from "@angular/router";
import { LoginService } from 'src/app/services/login/login.service';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { ConfigService } from '../../services/config/config.service';
import { SessionsService } from '../../services/sessions/sessions.service';
import { MatCard, MatCardContent } from '@angular/material/card';
import { NgStyle, NgClass } from '@angular/common';
import { LoaderComponent } from '../loader/loader.component';
import { CheckValidationCodeComponent } from '../check-validation-code/check-validation-code.component';
import { ListElementsComponent } from '../list-elements/list-elements.component';
import { EnvelopeComponent } from './envelope/envelope.component';
import { EndMessageComponent } from '../end-message/end-message.component';
import { SatisfactionCheckComponent } from '../satisfaction-check/satisfaction-check.component';
import { SafeHtmlPipe } from '../../shared/pipes/safe-html.pipe';

@Component({
    selector: 'ft-upload',
    templateUrl: './upload.component.html',
    styleUrls: ['./upload.component.scss'],
    imports: [FlowConfig, MatCard, MatCardContent, NgStyle, LoaderComponent, CheckValidationCodeComponent, ListElementsComponent, NgClass, EnvelopeComponent, EndMessageComponent, SatisfactionCheckComponent, TranslatePipe, SafeHtmlPipe]
})
export class UploadComponent implements OnInit, AfterViewInit, OnDestroy {
  private responsiveService = inject(ResponsiveService);
  private uploadManagerService = inject(UploadManagerService);
  private downloadManagerService = inject(DownloadManagerService);
  private fileManagerService = inject(FileManagerService);
  translateService = inject(TranslateService);
  private uploadService = inject(UploadService);
  private loginService = inject(LoginService);
  private languageSelectionService = inject(LanguageSelectionService);
  private configService = inject(ConfigService);
  private titleService = inject(Title);
  private _snackBar = inject(MatSnackBar);
  private router = inject(Router);
  private sessionsService = inject(SessionsService);
  private readonly destroyRef = inject(DestroyRef);

  readonly screenWidth = this.responsiveService.screenWidth;
  readonly uploadStarted = signal<boolean>(false);
  readonly uploadFinished = signal<boolean>(false);
  readonly uploadValidated = signal<boolean>(false);
  readonly uploadFailed = signal<boolean>(false);
  readonly publicLink = signal<boolean>(false);
  transfertSubscription: Subscription = new Subscription;
  uploadSubscription: Subscription = new Subscription;
  readonly senderEmail = signal<string>(undefined);
  readonly availabilityDate = signal<Date>(undefined);
  availabilityDays: number;
  private readonly uploadFragment = viewChild<ElementRef>('upload');
  readonly flow = viewChild<FlowConfig>('flow');
  flowConfig: any;
  readonly hasFiles = this.fileManagerService.hasFiles;
  readonly listExpanded = signal<boolean>(false);
  enclosureId: string = '';
  canReset: boolean = false;
  readonly showCode = signal<boolean>(false);
  readonly langueCourriels = this.uploadService.langueCourriels;
  private readonly ssoUploadRunning = signal(false);
  readonly config = this.configService.configInfo;

  constructor() {
    effect(() => {
      const _envelopeInfos = this.uploadManagerService.envelopeInfos();
      if (_envelopeInfos && _envelopeInfos.from) {
        this.senderEmail.set(_envelopeInfos.from.toLowerCase());
        if (_envelopeInfos.parameters) {
          this.availabilityDays = _envelopeInfos.parameters.expiryDays;
        }
      }
    });

    
    effect(() => {
      if (this.ssoUploadRunning() && this.loginService.tokenInfo()?.fromSso) {
        this.flow().flowJs.opts.headers = {
          Authorization: 'Bearer ' + this.loginService.getSsoToken()
        };
      }
    });
  }

  ngOnInit(): void {



    this.titleService.setTitle("France transfert - Résultat d'un envoi");
    this.flowConfig = FLOW_CONFIG;
    this.flowConfig.headers = {
      ...this.flowConfig.headers,
      'x-session-id': this.sessionsService.sessionId
    };
    this.reset();
  }

  ngAfterViewInit() {
    const tree = this.router.parseUrl(this.router.url);
    if (tree.fragment) {
      this.scrollTo(tree.fragment);
    }

  }

  scrollTo(_anchor: string) {
    switch (_anchor) {
      case 'upload':
        this.uploadFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
    }
  }


  reset() {
    this.uploadSubscription.unsubscribe();
    if (this.transfertSubscription) {
      this.transfertSubscription.unsubscribe();
    }
    this.enclosureId = '';
    this.uploadStarted.set(false);
    this.uploadFinished.set(false);
    this.uploadValidated.set(false);
    this.showCode.set(false);
    this.uploadFailed.set(false);
    this.publicLink.set(false);
    this.uploadManagerService.uploadInfos.set(null);
    this.uploadManagerService.uploadError.set(null);
    this.downloadManagerService.downloadError.set(null);
    //Reset token
    if (!this.canReset) {
      this.uploadManagerService.envelopeInfos.set(null);
      const flow = this.flow();
      if (flow?.flowJs) {
        this.fileManagerService.hasFiles.set(false);
        flow.cancel();
      }
    } else {
      this.flow().transfers$.pipe(take(1)).subscribe(transfer => {
        transfer.transfers.forEach(t => {
          t.flowFile.bootstrap();
        });
      });
    }
  }


  styleObject(): Object {
    if (this.screenWidth() === 'lg') {
      return { '-webkit-flex-direction': 'row' }
    }
    if (this.screenWidth() === 'md') {
      if (!this.uploadFinished() && !this.uploadStarted()) {
        return { '-webkit-flex-direction': 'row' }
      } else {
        return { '-webkit-flex-direction': 'column-reverse' }
      }
    }
    if (this.screenWidth() === 'sm') {
      if (this.uploadFinished() && this.uploadStarted()) {
        return { '-webkit-flex-direction': 'column' }
      } else {
        return { '-webkit-flex-direction': 'column-reverse' }
      }
    }
    return {}
  }

  onUploadStarted(event) {
    this.uploadStarted.set(event);
    this.upload();

  }

  onTransferFailed(event) {
    this.uploadFailed.set(true);
    this.uploadFinished.set(true);
    this.canReset = true;
    this.uploadSubscription.unsubscribe();
    this.ssoUploadRunning.set(false);
    this.flow().transfers$.pipe(take(1)).subscribe(transfer => {
      transfer.transfers.forEach(t => {
        t.flowFile.pause();
        t.flowFile.bootstrap();
      });
    });
  }

  onTransferCancelled(event) {
    this.uploadSubscription.unsubscribe();
    this.ssoUploadRunning.set(false);
    this.uploadStarted.set(!event);
    this.uploadValidated.set(!event);

  }

  onTransferFinished(event) {
    this.uploadSubscription.unsubscribe();
    this.ssoUploadRunning.set(false);
    this.uploadFinished.set(event);
    this.canReset = !event;


  }

  onTransferValidated(event) {
    if (event) {
      // this.uploadValidated = true;

      this.validateCode(event);

    }

  }

  onCheckTransferCancelled(event) {
    if (event) {
      this.uploadStarted.set(false);
    }
  }

  onSatisfactionCheckDone(event) {
    if (event) {
      this.uploadService.rate({
        plis: this.enclosureId, mail: this.uploadManagerService.envelopeInfos().from,
        message: event.message, satisfaction: event.satisfaction,
        ...this.loginService.tokenInfo()?.senderToken ? { token: this.loginService.tokenInfo()?.senderToken } : {},
      }).pipe(take(1))
        .subscribe((result: any) => {
          if (result) {
            this.openSnackBar(4000);
          }
          this.reset();
        });
    }
  }

  openSnackBar(duration: number) {
    this._snackBar.openFromComponent(SatisfactionMessageComponent, {
      panelClass: 'panel-success',
      duration: duration
    });
  }

  onListExpanded(event) {
    this.listExpanded.set(event);
  }

  ispublicLink(val: any) {
    if (val === 'link')
      this.publicLink.set(true);
  }

  async upload(): Promise<any> {
    let transfers: UploadState = await this.uploadManagerService.getRxValue(this.fileManagerService.transfers());
    this.uploadService
      .sendTree({
        transfers: transfers.transfers,
        ...this.uploadManagerService.envelopeInfos().type === 'mail' ? { emails: this.uploadManagerService.envelopeInfos().to } : {},
        message: this.uploadManagerService.envelopeInfos().message,
        subject: this.uploadManagerService.envelopeInfos().subject,
        senderMail: this.uploadManagerService.envelopeInfos().from.toLowerCase(),
        ...this.uploadManagerService.envelopeInfos().parameters?.password ? { password: this.uploadManagerService.envelopeInfos().parameters.password } : { password: '' },
        ...this.uploadManagerService.envelopeInfos().parameters?.expiryDays ? { expiryDays: this.uploadManagerService.envelopeInfos().parameters.expiryDays } : { expiryDays: 30 },
        ...this.uploadManagerService.envelopeInfos().type === 'link' ? { publicLink: true } : { publicLink: false },
        ...this.loginService.tokenInfo()?.senderToken ? { senderToken: this.loginService.tokenInfo()?.senderToken } : {},
        ...this.uploadManagerService.envelopeInfos().parameters?.zipPassword ? { zipPassword: this.uploadManagerService.envelopeInfos().parameters.zipPassword } : { zipPassword: false },
        ...this.uploadManagerService.envelopeInfos().parameters?.langueCourriels ? { langueCourriels: this.uploadManagerService.envelopeInfos().parameters.langueCourriels.code } : { langueCourriels: this.langueCourriels() },
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result: any) => {
        if (result && result?.canUpload == true) {
          this.uploadManagerService.uploadInfos.set(result);
          this.uploadValidated.set(true);
          this.showCode.set(false);
          this.availabilityDate.set(result.expireDate);
          this.ispublicLink(this.uploadManagerService.envelopeInfos().type);
          this.beginUpload(result);
        } else {
          this.showCode.set(true);
          if (this.uploadManagerService.uploadInfos()) {
            if (this.uploadManagerService.uploadInfos().senderId && this.uploadManagerService.uploadInfos().senderToken) {
              this.validateCode();
            }
          }
        }
      });
  }


  async validateCode(code?: string): Promise<any> {
    let transfers: UploadState = await this.uploadManagerService.getRxValue(this.fileManagerService.transfers());
    this.uploadService
      .validateCode({
        ...code ? { code: code } : {},
        transfers: transfers.transfers,
        ...this.uploadManagerService.envelopeInfos().type === 'mail' ? { emails: this.uploadManagerService.envelopeInfos().to } : {},
        message: this.uploadManagerService.envelopeInfos().message,
        subject: this.uploadManagerService.envelopeInfos().subject,
        senderMail: this.uploadManagerService.envelopeInfos().from.toLowerCase(),
        ...this.uploadManagerService.envelopeInfos().parameters?.password ? { password: this.uploadManagerService.envelopeInfos().parameters.password } : { password: '' },
        ...this.uploadManagerService.envelopeInfos().parameters?.expiryDays ? { expiryDays: this.uploadManagerService.envelopeInfos().parameters.expiryDays } : { expiryDays: 30 },
        ...this.uploadManagerService.envelopeInfos().type === 'link' ? { publicLink: true } : { publicLink: false },
        ...this.uploadManagerService.envelopeInfos().parameters?.zipPassword ? { zipPassword: this.uploadManagerService.envelopeInfos().parameters.zipPassword } : { zipPassword: false },
        ...this.uploadManagerService.envelopeInfos().parameters?.langueCourriels ? { langueCourriels: this.uploadManagerService.envelopeInfos().parameters.langueCourriels.code } : { langueCourriels: this.langueCourriels() },

      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result: any) => {
        this.uploadManagerService.uploadInfos.set(result);
        this.uploadValidated.set(true);
        this.availabilityDate.set(result.expireDate);
        this.ispublicLink(this.uploadManagerService.envelopeInfos().type);
        if (!this.loginService.connectCheck()) {
          this.loginService.tokenInfo.set(null);
        }
        this.beginUpload(result);
      });
  }

  beginUpload(result) {
    let token = '';
    if (this.transfertSubscription) {
      this.transfertSubscription.unsubscribe();
    }
    if (this.loginService.isLoggedIn()) {
      token = this.loginService.tokenInfo().senderToken;
      this.ssoUploadRunning.set(this.loginService.isSso());
    } else {
      token = this.uploadManagerService.uploadInfos().senderToken;
    }

    this.enclosureId = result.enclosureId;
    const flow = this.flow();
    flow.flowJs.opts.query = {
      enclosureId: result.enclosureId,
      senderId: this.uploadManagerService.envelopeInfos().from.toLowerCase(),
      senderToken: token,
    };
    if (this.loginService.isLoggedIn() && this.loginService.tokenInfo().fromSso) {
      flow.flowJs.opts.headers = {
        Authorization: 'Bearer ' + this.loginService.getSsoToken()
      };
    }

    this.transfertSubscription = flow.transfers$.subscribe((uploadState: UploadState) => {
      this.fileManagerService.uploadProgress.set(uploadState);
    });
    this.uploadSubscription = flow.events$.subscribe((event) => {
      if (event.type === 'fileRetry') {
        event.event[1]['tested'] = false;
      }
    });
    flow.upload();
  }

  ngOnDestroy() {
    this.transfertSubscription.unsubscribe();
    this.uploadSubscription.unsubscribe();
  }
}

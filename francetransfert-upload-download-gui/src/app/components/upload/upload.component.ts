/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { FlowDirective, UploadState } from '@flowjs/ngx-flow';
import { Subject } from 'rxjs/internal/Subject';
import { Subscription } from 'rxjs/internal/Subscription';
import { take, takeUntil } from 'rxjs/operators';
import { DownloadManagerService, FileEncryptionService, FileManagerService, LanguageSelectionService, ResponsiveService, UploadManagerService, UploadService } from 'src/app/services';
import { EncryptStreamInput } from 'src/app/services/file-encryption/file-encryption.service';
import { FLOW_CONFIG } from 'src/app/shared/config/flow-config';
import { MatLegacySnackBar as MatSnackBar } from "@angular/material/legacy-snack-bar";
import { SatisfactionMessageComponent } from "../satisfaction-message/satisfaction-message.component";
import { Router } from "@angular/router";
import { LoginService } from 'src/app/services/login/login.service';
import { TranslateService } from '@ngx-translate/core';
import { ConfigService } from '../../services/config/config.service';

@Component({
  selector: 'ft-upload',
  templateUrl: './upload.component.html',
  styleUrls: ['./upload.component.scss']
})
export class UploadComponent implements OnInit, AfterViewInit, OnDestroy {

  private onDestroy$: Subject<void> = new Subject();
  checkConnect: boolean;
  isMobile: boolean = false;
  screenWidth: string;
  uploadStarted: boolean = false;
  uploadFinished: boolean = false;
  uploadValidated: boolean = false;
  uploadFailed: boolean = false;
  publicLink: boolean = false;
  uploadManagerSubscription: Subscription = new Subscription;
  responsiveSubscription: Subscription = new Subscription;
  fileManagerSubscription: Subscription = new Subscription;
  transfertSubscription: Subscription = new Subscription;
  uploadSubscription: Subscription = new Subscription;
  loginSubscription: Subscription = new Subscription;
  langSubscription: Subscription = new Subscription;
  senderEmail: string;
  availabilityDate: Date;
  availabilityDays: number;
  @ViewChild('upload') private uploadFragment: ElementRef;
  @ViewChild('flow')
  flow: FlowDirective;
  flowConfig: any;
  hasFiles: boolean = false;
  listExpanded: boolean = false;
  enclosureId: string = '';
  canReset: boolean = false;
  showCode: boolean = false;
  langueCourriels: any;
  refreshUpdateSubscription: Subscription = new Subscription;
  config: any;
  configSubscription: Subscription;
  encrypting: boolean = false;
  encryptingProgress: number = 0;
  encryptedShareUrl: string = '';
  encryptedShareMailto: string = '';
  webShareAvailable: boolean = typeof navigator !== 'undefined' && typeof (navigator as any).share === 'function';


  constructor(private responsiveService: ResponsiveService,
    private uploadManagerService: UploadManagerService,
    private downloadManagerService: DownloadManagerService,
    private fileManagerService: FileManagerService,
    public translateService: TranslateService,
    private uploadService: UploadService,
    private loginService: LoginService,
    private languageSelectionService: LanguageSelectionService,
    private configService: ConfigService,
    private titleService: Title,
    private _snackBar: MatSnackBar,
    private router: Router,
    private changeDetectorRef: ChangeDetectorRef,
    private fileEncryptionService: FileEncryptionService) { }

  ngOnInit(): void {



    this.titleService.setTitle("France transfert - Résultat d'un envoi");
    this.onResize();
    this.flowConfig = FLOW_CONFIG;
    this.responsiveService.checkWidth();
    this.uploadManagerSubscription = this.uploadManagerService.envelopeInfos.subscribe(_envelopeInfos => {
      if (_envelopeInfos && _envelopeInfos.from) {
        this.senderEmail = _envelopeInfos.from.toLowerCase();
        if (_envelopeInfos.parameters) {
          this.availabilityDays = _envelopeInfos.parameters.expiryDays;
        }
      }
    });
    this.fileManagerSubscription = this.fileManagerService.hasFiles.subscribe(_hasFiles => {
      this.hasFiles = _hasFiles;
    });
    this.reset();


    this.loginSubscription = this.loginService.connectCheck.subscribe(checkConnect => {
      this.checkConnect = checkConnect;
    });

    this.langSubscription = this.uploadService.langueCourriels.subscribe(langueCourriels => {
      this.langueCourriels = langueCourriels;
      this.changeDetectorRef.detectChanges();
    });

    this.configSubscription = this.configService.configInfo.subscribe(config => {
      this.config = config;
      this.changeDetectorRef.detectChanges();
    });


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
        this.uploadFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
    }
  }


  reset() {
    this.cleanupTemporaryEncryptedFiles();
    this.encryptedShareUrl = '';
    this.encryptedShareMailto = '';
    this.uploadSubscription.unsubscribe();
    if (this.transfertSubscription) {
      this.transfertSubscription.unsubscribe();
    }
    this.enclosureId = '';
    this.uploadStarted = false;
    this.uploadFinished = false;
    this.uploadValidated = false;
    this.showCode = false;
    this.uploadFailed = false;
    this.publicLink = false;
    this.encrypting = false;
    this.encryptingProgress = 0;
    this.encryptedShareUrl = '';
    this.encryptedShareMailto = '';
    this.uploadManagerService.uploadInfos.next(null);
    this.uploadManagerService.encryptionEnabled.next(false);
    this.uploadManagerService.uploadError$.next(null);
    this.uploadManagerService.pliKeyBase64.next(null);
    this.downloadManagerService.downloadError$.next(null);
    //Reset token
    if (!this.canReset) {
      this.uploadManagerService.envelopeInfos.next(null);
      if (this.flow) {
        this.fileManagerService.hasFiles.next(false);
        this.flow.cancel();
      }
    } else {
      this.flow.transfers$.pipe(take(1)).subscribe(transfer => {
        transfer.transfers.forEach(t => {
          t.flowFile.bootstrap();
        });
      });
    }
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
      if (!this.uploadFinished && !this.uploadStarted) {
        return { '-webkit-flex-direction': 'row' }
      } else {
        return { '-webkit-flex-direction': 'column-reverse' }
      }
    }
    if (this.screenWidth === 'sm') {
      if (this.uploadFinished && this.uploadStarted) {
        return { '-webkit-flex-direction': 'column' }
      } else {
        return { '-webkit-flex-direction': 'column-reverse' }
      }
    }
    return {}
  }

  onUploadStarted(event) {
    this.uploadStarted = event;
    this.encryptThenUpload();
  }

  onTransferFailed(event) {
    this.cleanupTemporaryEncryptedFiles();
    this.uploadFailed = true;
    this.uploadFinished = true;
    this.canReset = true;
    this.uploadSubscription.unsubscribe();
    this.refreshUpdateSubscription.unsubscribe();
    this.flow.transfers$.pipe(take(1)).subscribe(transfer => {
      transfer.transfers.forEach(t => {
        t.flowFile.pause();
        t.flowFile.bootstrap();
      });
    });
  }

  onTransferCancelled(event) {
    this.cleanupTemporaryEncryptedFiles();
    this.uploadSubscription.unsubscribe();
    this.refreshUpdateSubscription.unsubscribe();
    this.uploadStarted = !event;
    this.uploadValidated = !event;

  }

  onTransferFinished(event) {
    this.cleanupTemporaryEncryptedFiles();
    this.uploadSubscription.unsubscribe();
    this.refreshUpdateSubscription.unsubscribe();
    this.uploadFinished = event;
    this.canReset = !event;
    if (event) {
      this.buildEncryptedShareUrl();
    }
  }

  private buildEncryptedShareUrl(): void {
    const pliKeyBase64 = this.uploadManagerService.pliKeyBase64.getValue();
    const enclosureId = this.uploadManagerService.uploadInfos.getValue()?.enclosureId
      ?? this.enclosureId;
    if (!pliKeyBase64 || !enclosureId) {
      this.encryptedShareUrl = '';
      this.encryptedShareMailto = '';
      return;
    }
    const origin = window.location.origin;
    this.encryptedShareUrl = `${origin}/download/download-info-public?enclosure=${enclosureId}#${pliKeyBase64}`;
    const subject = encodeURIComponent('Pli France transfert chiffré');
    const body = encodeURIComponent(`Bonjour,\n\nVoici un pli chiffré : ${this.encryptedShareUrl}\n\nLe lien contient la clé de déchiffrement. Conservez-le confidentiel.\n`);
    this.encryptedShareMailto = `mailto:?subject=${subject}&body=${body}`;
  }

  copyEncryptedShareUrl(): void {
    if (!this.encryptedShareUrl) return;
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(this.encryptedShareUrl).catch((err) => console.error('clipboard error', err));
    } else {
      const tmp = document.createElement('textarea');
      tmp.value = this.encryptedShareUrl;
      document.body.appendChild(tmp);
      tmp.select();
      try { document.execCommand('copy'); } finally { document.body.removeChild(tmp); }
    }
  }

  systemShareEncryptedUrl(): void {
    if (this.encryptedShareUrl && (navigator as any).share) {
      (navigator as any).share({
        title: 'Pli France transfert chiffré',
        text: 'Voici un pli chiffré (la clé est dans le lien).',
        url: this.encryptedShareUrl
      }).catch((err: unknown) => console.warn('share cancelled', err));
    }
  }

  onTransferValidated(event) {
    if (event) {
      // this.uploadValidated = true;

      this.validateCode(event);

    }

  }

  onCheckTransferCancelled(event) {
    if (event) {
      this.uploadStarted = false;
    }
  }

  onSatisfactionCheckDone(event) {
    if (event) {
      this.uploadService.rate({
        plis: this.enclosureId, mail: this.uploadManagerService.envelopeInfos.getValue().from,
        message: event.message, satisfaction: event.satisfaction,
        ...this.loginService.tokenInfo.getValue()?.senderToken ? { token: this.loginService.tokenInfo.getValue()?.senderToken } : {},
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
    this.listExpanded = event;
  }


  ispublicLink(val: any) {
    if (val === 'link')
      this.publicLink = true;
  }

  /** Sanitize a user-supplied string for use as a filename component. */
  private sanitizeForFilename(raw: string): string {
    return (
      raw
        .normalize('NFKD')
        .replace(/[/\\?%*:|"<>\x00-\x1f]/g, '')
        .replace(/\s+/g, '_')
        .slice(0, 60)
        .trim() || 'pli'
    );
  }

  /**
   * Pick the zip filename for an E2EE multi-file upload.
   * Priority:
   *   1. Single root folder selected → its name
   *   2. Subject filled by the user → `<subject>.zip`
   *   3. Otherwise → `francetransfert.zip`. Uniqueness across plis is
   *      enforced server-side by the enclosureId prefix in the S3 key, so
   *      there is no need to thread it into the client-side filename.
   */
  private pickEncryptedZipName(
    items: Array<{ file: File; relativePath: string }>
  ): string {
    const rootSegments = new Set(
      items
        .map(it => (it.relativePath || it.file.name).split('/')[0])
        .filter(Boolean)
    );
    if (rootSegments.size === 1) {
      return `${this.sanitizeForFilename(
        rootSegments.values().next().value as string
      )}.zip`;
    }
    const subject = this.uploadManagerService.envelopeInfos.getValue()?.subject;
    if (subject && subject.trim()) {
      return `${this.sanitizeForFilename(subject)}.zip`;
    }
    return 'francetransfert.zip';
  }

  /**
   * Build the streaming input the encryptor will pull plaintext bytes from:
   * - exactly one file → wrap its own `file.stream()` (zero copy, preserves
   *   name + extension)
   * - several files / a folder → run them through client-zip and forward its
   *   `ReadableStream` directly, so the zip is never materialised as a Blob.
   *   `predictLength` gives us the exact final zip size up front, which is
   *   needed both for the progress percentage and for the OPFS quota
   *   pre-check in TempEncryptedStorageService.
   *
   * This is what keeps multi-GB E2EE uploads from exploding the JS heap.
   */
  private async buildPlaintextSource(
    items: Array<{ file: File; relativePath: string }>
  ): Promise<EncryptStreamInput> {
    if (items.length === 1) {
      const file = items[0].file;
      return {
        stream: file.stream(),
        size: file.size,
        name: file.name,
        lastModified: file.lastModified,
      };
    }
    // Dynamic import: client-zip ships its own DEFLATE pipeline and is only
    // needed when a user encrypts a folder or several files. webpack splits
    // it into its own chunk so the main bundle (and the recipient/download
    // flow, which never zips) stays small. Cost: one extra HTTP/2 fetch the
    // first time a sender encrypts a multi-file pli — typically <100 ms.
    const clientZip = await import('client-zip');
    const entries = items.map(it => ({
      name: it.relativePath || it.file.name,
      lastModified: new Date(it.file.lastModified || Date.now()),
      input: it.file,
    }));
    const zipResponse = clientZip.downloadZip(entries);
    if (!zipResponse.body) {
      throw new Error('client-zip did not return a streamable body');
    }
    const zipSize = Number(clientZip.predictLength(entries));
    return {
      stream: zipResponse.body as ReadableStream<Uint8Array>,
      size: zipSize,
      name: this.pickEncryptedZipName(items),
      lastModified: Date.now(),
    };
  }

  /** Re-wrap an encrypted blob with a new filename (re-references the blob, no byte copy). */
  private renameFile(file: File, newName: string): File {
    return new File([file], newName, {
      type: file.type,
      lastModified: file.lastModified,
    });
  }

  async encryptThenUpload(): Promise<void> {
    const wantsEncryption = this.uploadManagerService.encryptionEnabled.getValue();
    this.encrypting = wantsEncryption;
    this.encryptingProgress = 0;
    const flowJs = this.flow?.flowJs;
    if (!flowJs?.files?.length) {
      this.encrypting = false;
      this.upload();
      return;
    }
    if (!wantsEncryption) {
      // Non-encrypted mode: keep the original FT flow (per-file upload, server
      // zips at the end). Nothing to do client-side.
      this.upload();
      return;
    }
    try {
      const flowFiles = Array.from(flowJs.files);
      const originalFiles = flowFiles.map((f: { file: File; relativePath?: string }) => ({
        file: f.file,
        relativePath: (f as { relativePath?: string }).relativePath || (f as any).name || f.file.name
      }));

      // The two `await`s below sequence the *setup*, not the bytes. The pipeline
      //
      //   File.stream()  →  client-zip (lazy)  →  secretstream encrypt  →  OPFS
      //
      // is fully streaming and pull-driven: nothing is computed until the
      // encryptor calls reader.read() and asks for the next chunk. Steady-state
      // RAM is bounded by one 5 MB chunk regardless of total input size, and
      // backpressure flows correctly (slow OPFS write → encryptor pauses
      // pulling → client-zip pauses → disk reads pause).
      const plaintext = await this.buildPlaintextSource(originalFiles);

      const pliKey = await this.fileEncryptionService.generatePliKey();
      const result = await this.fileEncryptionService.encryptStreamWithPliKey(
        plaintext, pliKey,
        (progress) => { this.encryptingProgress = progress; }
      );

      // Step 3: replace Flow.js queue with the single encrypted blob. Append
      // ".enc" to make explicit that the bytes are ciphertext — keeps S3 and
      // the rootFiles UI honest about what the server is actually holding.
      // The download flow strips ".enc" before saving the decrypted plaintext.
      const encryptedBlobName = `${result.encryptedFile.encryptedFile.name}.enc`;
      const renamedEncrypted = this.renameFile(result.encryptedFile.encryptedFile, encryptedBlobName);
      flowFiles.forEach((f) => flowJs.removeFile(f));
      flowJs.addFile(renamedEncrypted);

      // Step 4: stash pli key (base64url) — will be appended as URL fragment
      // on the success screen; never sent to the server.
      const pliKeyBase64 = await this.fileEncryptionService.exportPliKey(pliKey);
      this.uploadManagerService.pliKeyBase64.next(pliKeyBase64);

      this.encryptingProgress = 100;
      this.encrypting = false;
      this.upload();
    } catch (error) {
      console.error('encryptThenUpload failed', error);
      this.encrypting = false;
      this.encryptingProgress = 0;
      this.uploadManagerService.uploadError$.next({ statusCode: 0, message: 'ENCRYPTION_FAILED' });
      this.uploadStarted = false;
    }
  }

  async upload(): Promise<any> {
    let transfers: UploadState = await this.uploadManagerService.getRxValue(this.fileManagerService.transfers.getValue());
    const encrypted = this.uploadManagerService.encryptionEnabled.getValue();
    this.uploadService
      .sendTree({
        transfers: transfers.transfers,
        ...this.uploadManagerService.envelopeInfos.getValue().type === 'mail' ? { emails: this.uploadManagerService.envelopeInfos.getValue().to } : {},
        message: this.uploadManagerService.envelopeInfos.getValue().message,
        subject: this.uploadManagerService.envelopeInfos.getValue().subject,
        senderMail: this.uploadManagerService.envelopeInfos.getValue().from.toLowerCase(),
        ...this.uploadManagerService.envelopeInfos.getValue().parameters?.password ? { password: this.uploadManagerService.envelopeInfos.getValue().parameters.password } : { password: '' },
        ...this.uploadManagerService.envelopeInfos.getValue().parameters?.expiryDays ? { expiryDays: this.uploadManagerService.envelopeInfos.getValue().parameters.expiryDays } : { expiryDays: 30 },
        ...this.uploadManagerService.envelopeInfos.getValue().type === 'link' ? { publicLink: true } : { publicLink: false },
        ...this.loginService.tokenInfo.getValue()?.senderToken ? { senderToken: this.loginService.tokenInfo.getValue()?.senderToken } : {},
        ...this.uploadManagerService.envelopeInfos.getValue().parameters?.zipPassword ? { zipPassword: this.uploadManagerService.envelopeInfos.getValue().parameters.zipPassword } : { zipPassword: false },
        ...this.uploadManagerService.envelopeInfos.getValue().parameters?.langueCourriels ? { langueCourriels: this.uploadManagerService.envelopeInfos.getValue().parameters.langueCourriels.code } : { langueCourriels: this.langueCourriels },
        encrypted,
      })
      .pipe(takeUntil(this.onDestroy$))
      .subscribe((result: any) => {
        if (result && result?.canUpload == true) {
          this.uploadManagerService.uploadInfos.next(result);
          this.uploadValidated = true;
          this.showCode = false;
          this.availabilityDate = result.expireDate;
          this.ispublicLink(this.uploadManagerService.envelopeInfos.getValue().type);
          this.beginUpload(result);
        } else {
          this.showCode = true;
          if (this.uploadManagerService.uploadInfos.getValue()) {
            if (this.uploadManagerService.uploadInfos.getValue().senderId && this.uploadManagerService.uploadInfos.getValue().senderToken) {
              this.validateCode();
            }
          }
        }
      });
  }


  async validateCode(code?: string): Promise<any> {
    let transfers: UploadState = await this.uploadManagerService.getRxValue(this.fileManagerService.transfers.getValue());
    const encrypted = this.uploadManagerService.encryptionEnabled.getValue();
    this.uploadService
      .validateCode({
        ...code ? { code: code } : {},
        transfers: transfers.transfers,
        ...this.uploadManagerService.envelopeInfos.getValue().type === 'mail' ? { emails: this.uploadManagerService.envelopeInfos.getValue().to } : {},
        message: this.uploadManagerService.envelopeInfos.getValue().message,
        subject: this.uploadManagerService.envelopeInfos.getValue().subject,
        senderMail: this.uploadManagerService.envelopeInfos.getValue().from.toLowerCase(),
        ...this.uploadManagerService.envelopeInfos.getValue().parameters?.password ? { password: this.uploadManagerService.envelopeInfos.getValue().parameters.password } : { password: '' },
        ...this.uploadManagerService.envelopeInfos.getValue().parameters?.expiryDays ? { expiryDays: this.uploadManagerService.envelopeInfos.getValue().parameters.expiryDays } : { expiryDays: 30 },
        ...this.uploadManagerService.envelopeInfos.getValue().type === 'link' ? { publicLink: true } : { publicLink: false },
        ...this.uploadManagerService.envelopeInfos.getValue().parameters?.zipPassword ? { zipPassword: this.uploadManagerService.envelopeInfos.getValue().parameters.zipPassword } : { zipPassword: false },
        ...this.uploadManagerService.envelopeInfos.getValue().parameters?.langueCourriels ? { langueCourriels: this.uploadManagerService.envelopeInfos.getValue().parameters.langueCourriels.code } : { langueCourriels: this.langueCourriels },
        encrypted,
      })
      .pipe(takeUntil(this.onDestroy$))
      .subscribe((result: any) => {
        this.uploadManagerService.uploadInfos.next(result);
        this.uploadValidated = true;
        this.availabilityDate = result.expireDate;
        this.ispublicLink(this.uploadManagerService.envelopeInfos.getValue().type);
        if (this.checkConnect == false) {
          this.loginService.tokenInfo.next(null);
        }
        this.beginUpload(result);
      });
  }

  beginUpload(result) {
    let token = '';
    if (this.transfertSubscription) {
      this.transfertSubscription.unsubscribe();
    }
    if (this.loginService.isLoggedIn() && this.loginService.tokenInfo && this.loginService.tokenInfo.getValue()) {
      token = this.loginService.tokenInfo.getValue()?.senderToken;
      if (this.loginService.isSso()) {
        this.refreshUpdateSubscription = this.loginService.tokenInfo.subscribe(tokenInfo => {
          if (tokenInfo.fromSso) {
            this.flow.flowJs.opts.headers = { Authorization: 'Bearer ' + this.loginService.getSsoToken() };
          }
        });
      }
    } else {
      token = this.uploadManagerService.uploadInfos.getValue().senderToken;
    }

    this.enclosureId = result.enclosureId;
    this.flow.flowJs.opts.query = {
      enclosureId: result.enclosureId,
      senderId: this.uploadManagerService.envelopeInfos.getValue().from.toLowerCase(),
      senderToken: token,
    };
    if (this.loginService.isLoggedIn() && this.loginService.tokenInfo.getValue().fromSso) {
      this.flow.flowJs.opts.headers = { Authorization: 'Bearer ' + this.loginService.getSsoToken() };
    }

    this.transfertSubscription = this.flow.transfers$.subscribe((uploadState: UploadState) => {
      this.fileManagerService.uploadProgress.next(uploadState);
    });
    this.uploadSubscription = this.flow.events$.subscribe((event) => {
      if (event.type === 'fileRetry') {
        event.event[1]['tested'] = false;
      }
    });
    this.flow.upload();
  }

  private cleanupTemporaryEncryptedFiles(): void {
    void this.fileEncryptionService.cleanupTemporaryEncryptedFiles();
  }

  ngOnDestroy() {
    this.cleanupTemporaryEncryptedFiles();
    this.onDestroy$.next();
    this.onDestroy$.complete();
    this.responsiveSubscription.unsubscribe();
    this.uploadManagerSubscription.unsubscribe();
    this.fileManagerSubscription.unsubscribe();
    this.transfertSubscription.unsubscribe();
    this.loginSubscription.unsubscribe();
    this.langSubscription.unsubscribe();
    this.uploadSubscription.unsubscribe();
    this.refreshUpdateSubscription.unsubscribe();
    this.configSubscription.unsubscribe();
  }
}

/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, DestroyRef, OnInit, Signal, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { formSignal } from '../../shared/forms/form-signal';
import { FormBuilder, FormControl, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { Transfer } from '@flowjs/ngx-flow';
import moment from 'moment';
import { take } from 'rxjs/operators';
import { FTTransferModel } from 'src/app/models';
import { AdminService, ResponsiveService, UploadService } from 'src/app/services';
import { AdminAlertDialogComponent } from './admin-alert-dialog/admin-alert-dialog.component';
import { MyErrorStateMatcher } from "../upload/envelope/envelope-mail-form/envelope-mail-form.component";
import { MatSnackBar } from "@angular/material/snack-bar";
import { AdminEndMsgComponent } from "./admin-end-msg/admin-end-msg.component";
import { DateAdapter } from '@angular/material/core';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { Location, NgClass, DatePipe } from '@angular/common';
import { LoginService } from 'src/app/services/login/login.service';
import { DownloadEndMessageComponent } from '../download-end-message/download-end-message.component';
import { jsPDF } from 'jspdf';
import { FileSizePipe, FileTypePipe, FileUnitPipe } from 'src/app/shared/pipes';
import { environment } from '../../../environments/environment';
import { ConfigService } from '../../services/config/config.service';
import { MatCard, MatCardHeader, MatCardTitle, MatCardContent, MatCardActions } from '@angular/material/card';
import { MatList, MatListItem } from '@angular/material/list';
import { MatFormField, MatLabel, MatError } from '@angular/material/select';
import { MatInput } from '@angular/material/input';
import { MatDatepickerInput, MatDatepicker } from '@angular/material/datepicker';
import { MatIconButton, MatButton } from '@angular/material/button';
import { MatTooltip } from '@angular/material/tooltip';
import { MatIcon } from '@angular/material/icon';
import { MatAccordion, MatExpansionPanel, MatExpansionPanelHeader, MatExpansionPanelTitle } from '@angular/material/expansion';
import { FileItemComponent } from '../list-elements/file-item/file-item.component';
//import autoTable from 'jspdf-autotable';
@Component({
    selector: 'ft-admin',
    templateUrl: './admin.component.html',
    styleUrls: ['./admin.component.scss'],
    providers: [FileUnitPipe, FileSizePipe],
    imports: [MatCard, MatCardHeader, MatCardTitle, MatCardContent, MatList, MatListItem, MatFormField, MatInput, MatDatepickerInput, ReactiveFormsModule, MatDatepicker, MatIconButton, MatTooltip, MatIcon, NgClass, MatLabel, MatError, MatAccordion, MatExpansionPanel, MatExpansionPanelHeader, MatExpansionPanelTitle, FileItemComponent, MatCardActions, MatButton, DatePipe, TranslatePipe]
})
export class AdminComponent implements OnInit {
  private _adminService = inject(AdminService);
  private formBuilder = inject(FormBuilder);
  private _activatedRoute = inject(ActivatedRoute);
  private _router = inject(Router);
  private dialog = inject(MatDialog);
  private titleService = inject(Title);
  private uploadService = inject(UploadService);
  private _snackBar = inject(MatSnackBar);
  private _translate = inject(TranslateService);
  private location = inject(Location);
  private loginService = inject(LoginService);
  private responsiveService = inject(ResponsiveService);
  private configService = inject(ConfigService);
  private readonly destroyRef = inject(DestroyRef);

  params: Array<{ string: string }>;
  readonly fileInfos = signal<any>(undefined);
  pliInfos: any;
  readonly transfers = signal<Array<any>>([]);
  transfer: Array<any> = [];
  transferFolder: Array<any> = [];
  validUntilDate;
  minDate = new Date();
  maxDate = new Date();
  readonly errorMessage = signal('');
  readonly add = signal<boolean>(false);
  readonly close = signal<boolean>(false);
  emailFormControl: any;
  matcher = new MyErrorStateMatcher();
  readonly errorEmail = signal<boolean>(false);
  readonly errorValidEmail = signal<boolean>(false);
  senderOk: boolean = false;
  envelopeDestForm: Signal<FormGroup>;
  public readonly selectedDate = signal<Date>(new Date());
  readonly isMobile = this.responsiveService.isMobile;
  destinatairesList: any;

  readonly reciever = signal<boolean>(false);

  readonly numberOfDownload = signal<any>(undefined);
  readonly downloadDates = signal<any>(undefined);
  downloadDate: any;

  newUnit: any;
  unitSize: string;
  event: any;

  constructor() {
    effect(() => {
      if (this._adminService.adminError() === 401) {
        this.errorMessage.set('Existence_Pli');
      }
    });
  }

  ngOnInit(): void {

    this.initForm();
    this.transfers.set([]);
    this.titleService.setTitle('France transfert - Administration d\'un pli');
    this._activatedRoute.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params: Array<{ string: string }>) => {
      this.params = params;
      this.initData();
    });


  }

  onPickerClose() {
    // call api + reload
    let formattedDate = moment(this.validUntilDate.value).format('DD-MM-yyyy');
    const body = {
      "enclosureId": this.params['enclosure'],
      "token": this.params['token'] ? this.params['token'] : this.loginService.tokenInfo().senderToken,
      "newDate": formattedDate,
      "senderMail": this.loginService.tokenInfo() ? this.loginService.tokenInfo().senderMail : null,
    }
    this._adminService
      .updateExpiredDate(body)
      .pipe(take(1))
      .subscribe(response => {
        if (response) {
          this.fileInfos.update(fileInfos => ({ ...fileInfos, validUntilDate: this.validUntilDate.value }));
          //let temp = this.selectedDate;

          //this.initData();
        }
      });
  }

  deleteFile() {
    const dialogRef = this.dialog.open(AdminAlertDialogComponent, { data: 'deletePli' });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {

        const body = {
          "enclosureId": this.params['enclosure'],
          "token": this.params['token'] ? this.params['token'] : this.loginService.tokenInfo().senderToken,
          "senderMail": this.loginService.tokenInfo() ? this.loginService.tokenInfo().senderMail : null,
        }

        this._adminService
          .deleteFile(body)
          .pipe(take(1))
          .subscribe(response => {
            if (response) {
              if (this.params['token'] == null) {
                this.location.back();
              } else {
                this.initData();
              }
            }
          });
      }
    });
  }

  deleteRecipient(index, dest) {
    const dialogRef = this.dialog.open(AdminAlertDialogComponent, { data: 'deleteDest' });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        const body = {
          "enclosureId": this.params['enclosure'],
          "token": this.params['token'] ? this.params['token'] : this.loginService.tokenInfo().senderToken,
          "newRecipient": dest.recipientMail,
          "senderMail": this.loginService.tokenInfo() ? this.loginService.tokenInfo().senderMail : null,
        }
        this._adminService.deleteRecipient(body).
          pipe(take(1))
          .subscribe(response => {
            if (response) {
              this.fileInfos.update(fileInfos => ({
                ...fileInfos,
                recipientsMails: fileInfos.recipientsMails.filter((_, i) => i !== index),
                deletedRecipients: [...fileInfos.deletedRecipients, dest],
              }));
            }
          });

      }
    });

  }

  addRecipient() {
    this.add.set(!this.add());
    this.close.set(!this.close());
    // reset() also clears the errors set manually with setErrors()
    this.envelopeDestForm().reset({ email: '' });
  }

  onBlurDestinataires(event: any) {

    if (this.emailFormControl.errors == null) {
      this.errorEmail.set(false);
      this.destinatairesList = this.fileInfos().recipientsMails.map(ed => {
        return ed.recipientMail;
      })
      if (this.destinatairesList.length < this.configService.getMailLimit(this.loginService.isAgent())) {
        let found = this.destinatairesList.find(o => o === this.envelopeDestForm().get('email').value.toLowerCase());
        if (!found) {
          this.checkDestinataire(this.emailFormControl.value);
        }
        if (this.envelopeDestForm().controls['email'].getError('nbLimite') && this.envelopeDestForm().controls['email'].getError('nbLimite') == true) {
          this.envelopeDestForm().controls['email'].setErrors({ 'nbLimite': false });
        }

      } else {
        this.errorEmail.set(true);
        this.envelopeDestForm().controls['email'].markAsTouched();
        this.envelopeDestForm().get('email').setValue('');
        this.envelopeDestForm().controls['email'].setErrors({ 'nbLimite': true });
        event.preventDefault();
        return
      }
    } else {
      this.errorEmail.set(true);
      this.envelopeDestForm().controls['email'].markAsTouched();
      this.envelopeDestForm().controls['email'].setErrors({ emailError: true });
    }

  }

  initForm() {

    this.envelopeDestForm = formSignal(this.formBuilder.group({
      email: ['', { validators: [Validators.email], updateOn: 'blur' }],
    }));
    this.emailFormControl = this.envelopeDestForm().get('email');
  }

  checkDestinataire(email: any) {
    let destOk = false;
    if (this.emailFormControl.errors == null) {
      this.errorEmail.set(false);
      this.uploadService.validateMail([this.fileInfos().senderEmail]).pipe(
        take(1)).subscribe((isValid: boolean) => {
          this.senderOk = isValid;
          if (!this.senderOk && !this.errorEmail()) {
            this.uploadService.validateMail([email]).pipe(
              take(1)).subscribe((valid: boolean) => {
                destOk = valid;
                if (destOk) {
                  //appeler le back;
                  this.addNewRecipient(email);
                  this.errorValidEmail.set(false);
                  this.envelopeDestForm().controls['email'].markAsUntouched();
                  this.envelopeDestForm().controls['email'].setErrors({ emailNotValid: false });
                } else {
                  this.envelopeDestForm().controls['email'].markAsTouched();
                  this.envelopeDestForm().controls['email'].setErrors({ emailNotValid: true });
                  this.errorValidEmail.set(true);
                }
              })
          } else {
            if (!this.errorEmail()) {
              //appeler le back
              this.addNewRecipient(email);
            }
          }
        })
    } else {
      this.errorEmail.set(true);
      this.envelopeDestForm().controls['email'].markAsUntouched();
      this.envelopeDestForm().controls['email'].setErrors({ emailError: true });
    }

    this.envelopeDestForm().updateValueAndValidity();

  }

  resendLink(email: any) {
    const dialogRef = this.dialog.open(AdminAlertDialogComponent, { data: 'resendPli' });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        const body = {
          "enclosureId": this.params['enclosure'],
          "token": this.params['token'] ? this.params['token'] : this.loginService.tokenInfo().senderToken,
          "recipient": email,
          "senderMail": this.loginService.tokenInfo() ? this.loginService.tokenInfo().senderMail : null,
        }
        this._adminService
          .resendLink(body)
          .pipe(take(1))
          .subscribe(response => {
            if (response) {
              this.openSnackBarDownload(4000);

            }
          });
      }

    });

  }


  addNewRecipient(email: any) {
    const body = {
      "enclosureId": this.params['enclosure'],
      "token": this.params['token'] ? this.params['token'] : this.loginService.tokenInfo().senderToken,
      "newRecipient": email,
      "senderMail": this.loginService.tokenInfo() ? this.loginService.tokenInfo().senderMail : null,
    }
    this._adminService
      .addNewRecipient(body)
      .pipe(take(1))
      .subscribe(response => {
        if (response) {
          this.fileInfos.update(fileInfos => ({
            ...fileInfos,
            recipientsMails: [...fileInfos.recipientsMails, { recipientMail: email, numberOfDownloadPerRecipient: 0 }],
            deletedRecipients: fileInfos.deletedRecipients.filter(deleted => deleted.recipientMail !== email),
          }));
          this.envelopeDestForm().get('email').setValue('');
          this.openSnackBar();
        }
      });
  }

  openSnackBar() {
    this._snackBar.openFromComponent(AdminEndMsgComponent, {
      duration: 4000,
    });
  }

  //-------------navigate to download page------------
  navigateTo() {
    if (this.loginService.isLoggedIn()) {
      this._router.navigate(['/download'], {
        queryParams: {
          enclosure: this.params['enclosure'],
          recieved: false,
        },
        queryParamsHandling: 'merge',
      });
    }
    else {
      this._router.navigate(['/plis']);

    }
  }

  previousPage() {
    if (this.params['token'] == null) {
      this.location.back();
    }
    else {
      this._router.navigate(['/upload']);

    }
  }

  openSnackBarDownload(duration: number) {
    this._snackBar.openFromComponent(DownloadEndMessageComponent, {
      duration: duration
    });
  }

  toArray(downloadDates: object) {
    if (downloadDates && Object.keys(downloadDates).length > 0) {

      return Object.keys(downloadDates).map(key => ({ value: downloadDates[key] })).sort((a, b) => {
        return <any>new Date(a.value) - <any>new Date(b.value);
      });

    }
  }

  get translate(): TranslateService {
    return this._translate;
  }


  DupliquerDestinataires() {
    this._adminService.setDestinatairesList({
      destinataires: this.fileInfos().recipientsMails.map(ed => {
        return ed.recipientMail;
      }),

    });
    this._router.navigate(['/upload']);

  }

  initData() {
    if (this.params['enclosure'] && this.params['token']) {
      this._adminService
        .getFileInfos(this.params)
        .pipe(take(1))
        .subscribe(fileInfos => {
          this.applyFileInfos(fileInfos);
        });
    }
    else if (this.loginService.isLoggedIn() && this.params['token'] == null && this.params['enclosure']) {
      let enclosureId = this.params['enclosure'];
      if (!this.params['recieved']) {
        this.reciever.set(this.params['recieved']);
        this._adminService
          .getFileInfosConnect({
            senderMail: this.loginService.tokenInfo().senderMail,
            senderToken: this.loginService.tokenInfo().senderToken,
          }, enclosureId)
          .pipe(take(1))
          .subscribe(fileInfos => {
            this.applyFileInfos(fileInfos);
          });

      } else {
        this.reciever.set(this.params['recieved']);
        this._adminService
          .getFileInfosReciever({
            enclosure: this.params['enclosure'],
            senderMail: this.loginService.tokenInfo().senderMail,
            senderToken: this.loginService.tokenInfo().senderToken,
          })
          .pipe(take(1))
          .subscribe(fileInfos => {
            this.applyFileInfos(fileInfos);
            this.fileInfos().recipientsMails.forEach((element) => {
              this.numberOfDownload.set(element.numberOfDownloadPerRecipient);
              this.downloadDates.set(element.downloadDates);
            });
          });
      }
    }
    else {
      this._router.navigateByUrl('/error');
    }
  }

  private applyFileInfos(fileInfos: any) {
    this.fileInfos.set(fileInfos);
    this.updateAgent();
    // fresh array (not an in-place push): a reload after deleteFile() no longer duplicates the list
    this.transfers.set([
      ...fileInfos.rootFiles.map(file => ({ ...file, folder: false } as FTTransferModel<Transfer>)),
      ...fileInfos.rootDirs.map(file => ({ ...file, size: file.totalSize, folder: true } as FTTransferModel<Transfer>)),
    ]);
    this.validUntilDate = new FormControl(new Date(fileInfos.validUntilDate));
    this.setLimitDay(fileInfos.timestamp);
  }

  private updateAgent() {
    this.uploadService.validateMail([this.fileInfos().senderEmail]).pipe(take(1)).subscribe((isAllowed: boolean) => {
      this.loginService.isAgent.set(isAllowed);
    });
  }

  public getMailLimit(): number {
    return this.configService.getMailLimit(this.loginService.isAgent());
  }

  PDFinfo() {
    if (!this.reciever()) {
      this._adminService
        .getFileInfosConnect({
          senderMail: this.loginService.tokenInfo() ? this.loginService.tokenInfo().senderMail : null,
          senderToken: this.params['token'] ? this.params['token'] : this.loginService.tokenInfo().senderToken
        }, this.fileInfos().enclosureId)
        .pipe(take(1))
        .subscribe(pliInfos => {
          this.pliInfos = pliInfos;
          this._adminService.generatePDF(this.pliInfos, "namePDF-sent")

        });
    } else {
      this._adminService.generatePDF(this.fileInfos(), "namePDF-recieved")

    }

  }

  private setLimitDay(date) {
    let temp = moment(date);
    this.selectedDate.set(temp.toDate());
    this.maxDate = temp.add(this.configService.configInfo().uploadExpiredLimit, 'days').toDate();
  }



}



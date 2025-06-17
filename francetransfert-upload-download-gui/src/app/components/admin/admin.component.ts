/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { MatLegacyDialog as MatDialog } from '@angular/material/legacy-dialog';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { Transfer } from '@flowjs/ngx-flow';
import * as moment from 'moment';
import { Subject, Subscription } from 'rxjs';
import { take, takeUntil } from 'rxjs/operators';
import { FTTransferModel } from 'src/app/models';
import { AdminService, ResponsiveService, UploadService } from 'src/app/services';
import { AdminAlertDialogComponent } from './admin-alert-dialog/admin-alert-dialog.component';
import { MyErrorStateMatcher } from "../upload/envelope/envelope-mail-form/envelope-mail-form.component";
import { MatLegacySnackBar as MatSnackBar } from "@angular/material/legacy-snack-bar";
import { AdminEndMsgComponent } from "./admin-end-msg/admin-end-msg.component";
import { DateAdapter } from '@angular/material/core';
import { TranslateService } from '@ngx-translate/core';
import { Location } from '@angular/common';
import { LoginService } from 'src/app/services/login/login.service';
import { DownloadEndMessageComponent } from '../download-end-message/download-end-message.component';
import { jsPDF } from 'jspdf';
import * as pdfMake from "pdfmake/build/pdfmake";
import * as pdfFonts from "pdfmake/build/vfs_fonts";
import { FileSizePipe, FileTypePipe, FileUnitPipe } from 'src/app/shared/pipes';
//import autoTable from 'jspdf-autotable';
(pdfMake as any).vfs = pdfFonts.pdfMake.vfs;
@Component({
  selector: 'ft-admin',
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss'],
  providers: [FileUnitPipe, FileSizePipe]
})
export class AdminComponent implements OnInit, OnDestroy {

  private onDestroy$: Subject<void> = new Subject();
  params: Array<{ string: string }>;
  fileInfos: any;
  pliInfos: any;
  transfers: Array<any> = [];
  transfer: Array<any> = [];
  transferFolder: Array<any> = [];
  validUntilDate;
  minDate = new Date();
  maxDate = new Date();
  errorMessage = '';
  adminErrorsSubscription: Subscription;
  add: boolean = false;
  close: boolean = false;
  emailFormControl: any;
  matcher = new MyErrorStateMatcher();
  envelopeMailFormChangeSubscription: Subscription;
  errorEmail: boolean = false;
  errorValidEmail: boolean = false;
  senderOk: boolean = false;
  envelopeDestForm: FormGroup;
  public selectedDate: Date = new Date();
  responsiveSubscription: Subscription = new Subscription;
  isMobile: boolean = false;
  destinatairesList: any;

  reciever: boolean = false;

  numberOfDownload: any;
  downloadDates: any;
  downloadDate: any;

  newUnit: any;
  unitSize: string;
  event: any;

  constructor(private _adminService: AdminService, private formBuilder: FormBuilder,
    private _activatedRoute: ActivatedRoute,
    private _router: Router,
    private dialog: MatDialog,
    private titleService: Title,
    private uploadService: UploadService, private _snackBar: MatSnackBar,
    private _translate: TranslateService,
    private location: Location,
    private loginService: LoginService,
    private responsiveService: ResponsiveService,
    private adminService: AdminService,

  ) {
  }
  ngOnInit(): void {

    this.responsiveSubscription = this.responsiveService.getMobileStatus().subscribe(isMobile => {
      this.isMobile = isMobile;
    });

    this.initForm();
    this.transfers = [];
    this.titleService.setTitle('France transfert - Administration d\'un pli');
    this._activatedRoute.queryParams.pipe(takeUntil(this.onDestroy$)).subscribe((params: Array<{ string: string }>) => {
      this.params = params;
      this.initData();
    });
    this.adminErrorsSubscription = this._adminService.adminError$.subscribe(err => {
      if (err === 401) {
        this.errorMessage = 'Existence_Pli';
      }
    });


  }

  onPickerClose() {
    // call api + reload
    let formattedDate = moment(this.validUntilDate.value).format('DD-MM-yyyy');
    const body = {
      "enclosureId": this.params['enclosure'],
      "token": this.params['token'] ? this.params['token'] : this.loginService.tokenInfo.getValue().senderToken,
      "newDate": formattedDate,
      "senderMail": this.loginService.tokenInfo.getValue() ? this.loginService.tokenInfo.getValue().senderMail : null,
    }
    this._adminService
      .updateExpiredDate(body)
      .pipe(take(1))
      .subscribe(response => {
        if (response) {
          this.fileInfos.validUntilDate = this.validUntilDate.value;
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
          "token": this.params['token'] ? this.params['token'] : this.loginService.tokenInfo.getValue().senderToken,
          "senderMail": this.loginService.tokenInfo.getValue() ? this.loginService.tokenInfo.getValue().senderMail : null,
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

  ngOnDestroy() {
    this.onDestroy$.next();
    this.onDestroy$.complete();
    this.adminErrorsSubscription.unsubscribe();
    this.responsiveSubscription.unsubscribe();
  }

  deleteRecipient(index, dest) {
    const dialogRef = this.dialog.open(AdminAlertDialogComponent, { data: 'deleteDest' });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        const body = {
          "enclosureId": this.params['enclosure'],
          "token": this.params['token'] ? this.params['token'] : this.loginService.tokenInfo.getValue().senderToken,
          "newRecipient": dest.recipientMail,
          "senderMail": this.loginService.tokenInfo.getValue() ? this.loginService.tokenInfo.getValue().senderMail : null,
        }
        this._adminService.deleteRecipient(body).
          pipe(take(1))
          .subscribe(response => {
            if (response) {
              this.fileInfos.recipientsMails.splice(index, 1);
              this.fileInfos.deletedRecipients.push(dest);
            }
          });;

      }
    });

  }

  addRecipient() {
    this.add = !this.add;
    this.close = !this.close;
    this.initForm();
  }

  onBlurDestinataires() {


    if (this.emailFormControl.errors == null) {
      this.errorEmail = false;
      this.destinatairesList = this.fileInfos.recipientsMails.map(ed => {
        return ed.recipientMail;
      })
      if (this.destinatairesList.length < 100) {
        let found = this.destinatairesList.find(o => o === this.envelopeDestForm.get('email').value.toLowerCase());
        if (!found) {
          this.checkDestinataire(this.emailFormControl.value);
        }
        if (this.envelopeDestForm.controls['email'].getError('nbLimite') && this.envelopeDestForm.controls['email'].getError('nbLimite') == true) {
          this.envelopeDestForm.controls['email'].setErrors({ 'nbLimite': false });
        }

      } else {
        this.errorEmail = true;
        this.envelopeDestForm.controls['email'].markAsTouched();
        this.envelopeDestForm.get('email').setValue('');
        this.envelopeDestForm.controls['email'].setErrors({ 'nbLimite': true });
      }
    } else {
      this.errorEmail = true;
      this.envelopeDestForm.controls['email'].markAsTouched();
      this.envelopeDestForm.controls['email'].setErrors({ emailError: true });
    }

  }

  initForm() {

    this.envelopeDestForm = this.formBuilder.group({
      email: ['', { validators: [Validators.email], updateOn: 'blur' }],
    });
    this.emailFormControl = this.envelopeDestForm.get('email');
    this.envelopeMailFormChangeSubscription = this.emailFormControl.valueChanges
      .subscribe(() => {
      });
  }

  checkDestinataire(email: any) {
    let destOk = false;
    if (this.emailFormControl.errors == null) {
      this.errorEmail = false;
      this.uploadService.validateMail([this.fileInfos.senderEmail]).pipe(
        take(1)).subscribe((isValid: boolean) => {
          this.senderOk = isValid;
          if (!this.senderOk && !this.errorEmail) {
            this.uploadService.validateMail([email]).pipe(
              take(1)).subscribe((valid: boolean) => {
                destOk = valid;
                if (destOk) {
                  //appeler le back;
                  this.addNewRecipient(email);
                  this.errorValidEmail = false;
                  this.envelopeDestForm.controls['email'].markAsUntouched();
                  this.envelopeDestForm.controls['email'].setErrors({ emailNotValid: false });
                } else {
                  this.envelopeDestForm.controls['email'].markAsTouched();
                  this.envelopeDestForm.controls['email'].setErrors({ emailNotValid: true });
                  this.errorValidEmail = true;
                }
              })
          } else {
            if (!this.errorEmail) {
              //appeler le back
              this.addNewRecipient(email);
            }
          }
        })
    } else {
      this.errorEmail = true;
      this.envelopeDestForm.controls['email'].markAsUntouched();
      this.envelopeDestForm.controls['email'].setErrors({ emailError: true });
    }

    this.envelopeDestForm.updateValueAndValidity();

  }

  resendLink(email: any) {
    const dialogRef = this.dialog.open(AdminAlertDialogComponent, { data: 'resendPli' });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        const body = {
          "enclosureId": this.params['enclosure'],
          "token": this.params['token'] ? this.params['token'] : this.loginService.tokenInfo.getValue().senderToken,
          "recipient": email,
          "senderMail": this.loginService.tokenInfo.getValue() ? this.loginService.tokenInfo.getValue().senderMail : null,
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
      "token": this.params['token'] ? this.params['token'] : this.loginService.tokenInfo.getValue().senderToken,
      "newRecipient": email,
      "senderMail": this.loginService.tokenInfo.getValue() ? this.loginService.tokenInfo.getValue().senderMail : null,
    }
    this._adminService
      .addNewRecipient(body)
      .pipe(take(1))
      .subscribe(response => {
        if (response) {
          this.fileInfos.recipientsMails.push({ recipientMail: email, numberOfDownloadPerRecipient: 0 });
          for (let i = 0; i < this.fileInfos.deletedRecipients.length; i++) {
            if (this.fileInfos.deletedRecipients[i] === email) {
              this.fileInfos.deletedRecipients.splice(i, 1);
            }
          }
          this.envelopeDestForm.get('email').setValue('');
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
    this.adminService.setDestinatairesList({
      destinataires: this.fileInfos.recipientsMails.map(ed => {
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
          this.fileInfos = fileInfos;
          this.fileInfos.rootFiles.map(file => {
            this.transfers.push({ ...file, folder: false } as FTTransferModel<Transfer>);
          });
          this.fileInfos.rootDirs.map(file => {
            this.transfers.push({ ...file, size: file.totalSize, folder: true } as FTTransferModel<Transfer>);
          });
          this.validUntilDate = new FormControl(new Date(this.fileInfos.validUntilDate));
          this.setLimitDay(this.fileInfos.timestamp);
        });
    }
    else if (this.loginService.isLoggedIn() && this.params['token'] == null && this.params['enclosure']) {
      let enclosureId = this.params['enclosure'];
      if (!this.params['recieved']) {
        this.reciever = this.params['recieved'];
        this._adminService
          .getFileInfosConnect({
            senderMail: this.loginService.tokenInfo.getValue().senderMail,
            senderToken: this.loginService.tokenInfo.getValue().senderToken,
          }, enclosureId)
          .pipe(take(1))
          .subscribe(fileInfos => {
            this.fileInfos = fileInfos;
            this.fileInfos.rootFiles.map(file => {
              this.transfers.push({ ...file, folder: false } as FTTransferModel<Transfer>);
            });
            this.fileInfos.rootDirs.map(file => {
              this.transfers.push({ ...file, size: file.totalSize, folder: true } as FTTransferModel<Transfer>);
            });
            this.validUntilDate = new FormControl(new Date(this.fileInfos.validUntilDate));
            this.setLimitDay(this.fileInfos.timestamp);
          });

      } else {
        this.reciever = this.params['recieved'];
        this._adminService
          .getFileInfosReciever({
            enclosure: this.params['enclosure'],
            senderMail: this.loginService.tokenInfo.getValue().senderMail,
            senderToken: this.loginService.tokenInfo.getValue().senderToken,
          })
          .pipe(take(1))
          .subscribe(fileInfos => {
            this.fileInfos = fileInfos;
            this.fileInfos.rootFiles.map(file => {
              this.transfers.push({ ...file, folder: false } as FTTransferModel<Transfer>);
            });
            this.fileInfos.rootDirs.map(file => {
              this.transfers.push({ ...file, size: file.totalSize, folder: true } as FTTransferModel<Transfer>);
            });
            this.validUntilDate = new FormControl(new Date(this.fileInfos.validUntilDate));
            this.setLimitDay(this.fileInfos.timestamp);
            this.fileInfos.recipientsMails.forEach((element) => {
              this.numberOfDownload = element.numberOfDownloadPerRecipient;
              this.downloadDates = element.downloadDates;
            });
          });
      }
    }
    else {
      this._router.navigateByUrl('/error');
    }
  }

  PDFinfo() {
    if (!this.reciever) {
      this._adminService
        .getFileInfosConnect({
          senderMail: this.loginService.tokenInfo.getValue() ? this.loginService.tokenInfo.getValue().senderMail : null,
          senderToken: this.params['token'] ? this.params['token'] : this.loginService.tokenInfo.getValue().senderToken
        }, this.fileInfos.enclosureId)
        .pipe(take(1))
        .subscribe(pliInfos => {
          this.pliInfos = pliInfos;
          this.adminService.generatePDF(this.pliInfos, "namePDF-sent")

        });
    } else {
      this.adminService.generatePDF(this.fileInfos, "namePDF-recieved")

    }

  }

  private setLimitDay(date) {
    let temp = moment(date);
    this.selectedDate = temp.toDate();
    this.maxDate = temp.add(90, 'days').toDate();
  }



}



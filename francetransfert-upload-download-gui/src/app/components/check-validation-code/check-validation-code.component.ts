/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatLegacyDialog as MatDialog } from '@angular/material/legacy-dialog';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';
import { FTErrorModel } from 'src/app/models';
import { DownloadManagerService, UploadManagerService } from 'src/app/services';
import { LoginService } from 'src/app/services/login/login.service';
import { ConfirmAlertDialogComponent } from './confirm-alert-dialog/confirm-alert-dialog.component';

@Component({
  selector: 'ft-check-validation-code',
  templateUrl: './check-validation-code.component.html',
  styleUrls: ['./check-validation-code.component.scss']
})
export class CheckValidationCodeComponent implements OnInit, OnDestroy {

  @Input() component: 'upload' | 'download';
  @Input() email: string;
  @Input() publicLink: boolean;

  verificationCodeForm: FormGroup;
  @Output() transferCancelled: EventEmitter<boolean> = new EventEmitter();
  @Output() transferValidated: EventEmitter<string> = new EventEmitter();
  @Output() dowloadValidated: EventEmitter<string> = new EventEmitter();
  errorSubscription: Subscription = new Subscription();
  errorDLSubscription: Subscription = new Subscription();
  error: FTErrorModel;
  hide = true;
  errorMessage: any;
  buttonDisable = false;
  showPassword = true;
  isLoggedIn = false;
  label = "Afficher le mot de passe";

  constructor(private fb: FormBuilder, private uploadManagerService: UploadManagerService,
    private downloadManagerService: DownloadManagerService, private loginService: LoginService,
    private translate: TranslateService,
    private dialog: MatDialog) { }

  ngOnInit(): void {
    this.initForm();
    this.isLoggedIn = this.loginService.isLoggedIn();
    if (this.component === 'upload' && (this.isLoggedIn == true && this.loginService.isSso())) {
      this.loginService.logout();
    }
    this.errorSubscription = this.uploadManagerService.uploadError$.subscribe(error => {
      if (error) {
        this.translate.stream(error.message).pipe(take(1)).subscribe(v => {
          this.errorMessage = v;
        })
        this.error = { statusCode: error.statusCode, message: this.errorMessage, codeTryCount: error.codeTryCount };
      }
      this.buttonDisable = false;
    });
    this.errorDLSubscription = this.downloadManagerService.downloadError$.subscribe(error => {
      if (error) {
        this.translate.stream(error.message).pipe(take(1)).subscribe(v => {
          this.errorMessage = v;
        })
        this.error = { statusCode: error.statusCode, message: this.errorMessage, codeTryCount: error.codeTryCount };
      }
      this.buttonDisable = false;
    });
  }

  ngOnDestroy(): void {
    this.errorSubscription.unsubscribe();
    this.errorDLSubscription.unsubscribe();
  }

  initForm() {

    this.verificationCodeForm = this.fb.group({
      verificationCode: ['', [Validators.required]],
      connectCheck: [true, [Validators.required]],
    });
  }

  // convenience getter for easy access to form fields
  get f() { return this.verificationCodeForm.controls; }

  validateForm() {
    // stop here if form is invalid
    if (this.verificationCodeForm.invalid) {
      return;
    }
    this.loginService.connectCheck.next(this.verificationCodeForm.get('connectCheck').value);
    if (this.component === 'upload') {
      this.transferValidated.emit(this.verificationCodeForm.get('verificationCode').value);
      this.buttonDisable = true;
    }
    if (this.component === 'download') {
      this.dowloadValidated.emit(this.verificationCodeForm.get('verificationCode').value);
      this.buttonDisable = true;
    }

  }

  labelChanges(){
    if(this.hide){
      this.label = "Afficher le mot de passe"
    }else{
      this.label = "Masquer le mot de passe"
    }

  }

  backToHome() {
    window.location.reload();
  }

  cancel() {
    const dialogRef = this.dialog.open(ConfirmAlertDialogComponent);

    dialogRef.afterClosed().pipe(take(1)).subscribe(result => {
      if (result) {
        this.transferCancelled.emit(true);
      }
    });
  }
}

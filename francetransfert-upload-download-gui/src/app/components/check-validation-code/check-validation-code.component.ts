/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, OnInit, effect, inject, input, output, signal, untracked, Signal } from '@angular/core';
import { formSignal } from '../../shared/forms/form-signal';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { take } from 'rxjs/operators';
import { FTErrorModel } from 'src/app/models';
import { DownloadManagerService, UploadManagerService } from 'src/app/services';
import { LoginService } from 'src/app/services/login/login.service';
import { ConfirmAlertDialogComponent } from './confirm-alert-dialog/confirm-alert-dialog.component';
import { MatCard, MatCardContent, MatCardActions } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatFormField, MatLabel, MatError, MatSuffix } from '@angular/material/select';
import { MatInput } from '@angular/material/input';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatButton, MatIconButton } from '@angular/material/button';

@Component({
    selector: 'ft-check-validation-code',
    templateUrl: './check-validation-code.component.html',
    styleUrls: ['./check-validation-code.component.scss'],
    imports: [MatCard, ReactiveFormsModule, MatCardContent, MatIcon, MatFormField, MatLabel, MatInput, MatError, MatCheckbox, MatCardActions, MatButton, MatIconButton, MatSuffix, TranslatePipe]
})
export class CheckValidationCodeComponent implements OnInit {
  private fb = inject(FormBuilder);
  private uploadManagerService = inject(UploadManagerService);
  private downloadManagerService = inject(DownloadManagerService);
  private loginService = inject(LoginService);
  private translate = inject(TranslateService);
  private dialog = inject(MatDialog);


  readonly component = input<'upload' | 'download'>(undefined);
  readonly email = input<string>(undefined);
  readonly publicLink = input<boolean>(undefined);

  verificationCodeForm: Signal<FormGroup>;
  readonly transferCancelled = output<boolean>();
  readonly transferValidated = output<string>();
  readonly dowloadValidated = output<string>();
  readonly error = signal<FTErrorModel>(undefined);
  hide = true;
  readonly errorMessage = signal<any>(undefined);
  readonly buttonDisable = signal(false);
  showPassword = true;
  readonly isLoggedIn = signal(false);
  label = "Afficher le mot de passe";

  constructor() {
    effect(() => this.onError(this.uploadManagerService.uploadError()));
    effect(() => this.onError(this.downloadManagerService.downloadError()));
  }

  private onError(error: FTErrorModel | null) {
    untracked(() => {
      if (error) {
        this.translate.stream(error.message).pipe(take(1)).subscribe(v => {
          this.errorMessage.set(v);
        })
        this.error.set({ statusCode: error.statusCode, message: this.errorMessage(), codeTryCount: error.codeTryCount });
      }
      this.buttonDisable.set(false);
    });
  }

  ngOnInit(): void {
    this.initForm();
    this.isLoggedIn.set(this.loginService.isLoggedIn());
    if (this.component() === 'upload' && (this.isLoggedIn() == true && this.loginService.isSso())) {
      this.loginService.logout();
    }
  }

  initForm() {

    this.verificationCodeForm = formSignal(this.fb.group({
      verificationCode: ['', [Validators.required]],
      connectCheck: [true, [Validators.required]],
    }));
  }

  // convenience getter for easy access to form fields
  get f() { return this.verificationCodeForm().controls; }

  validateForm() {
    // stop here if form is invalid
    if (this.verificationCodeForm().invalid) {
      return;
    }
    this.loginService.connectCheck.set(this.verificationCodeForm().get('connectCheck').value);
    const component = this.component();
    if (component === 'upload') {
      this.transferValidated.emit(this.verificationCodeForm().get('verificationCode').value);
      this.buttonDisable.set(true);
    }
    if (component === 'download') {
      this.dowloadValidated.emit(this.verificationCodeForm().get('verificationCode').value);
      this.buttonDisable.set(true);
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

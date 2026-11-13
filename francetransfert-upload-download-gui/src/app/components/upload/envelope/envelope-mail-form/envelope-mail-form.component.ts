/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, ElementRef, OnDestroy, OnInit, effect, inject, input, output, untracked, viewChild, Signal } from '@angular/core'
import { formSignal } from '../../../../shared/forms/form-signal';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators, ReactiveFormsModule } from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { saveAs } from 'file-saver';
import { Subscription } from 'rxjs';
import { MailingListManagerComponent } from 'src/app/components/mailing-list-manager/mailing-list-manager.component';
import { MailInfosModel } from 'src/app/models';
import { AdminService, UploadManagerService, UploadService } from 'src/app/services';
import { LoginService } from 'src/app/services/login/login.service';
import { MailAsyncValidator } from 'src/app/shared/validators/mail-validator';
import { QuotaAsyncValidator } from 'src/app/shared/validators/quota-validator';
import { ConfigService } from '../../../../services/config/config.service';
import { NgClass } from '@angular/common';
import { MatFormField, MatLabel, MatError, MatSuffix } from '@angular/material/select';
import { MatInput } from '@angular/material/input';
import { MatIconButton } from '@angular/material/button';
import { MatTooltip } from '@angular/material/tooltip';
import { MatIcon } from '@angular/material/icon';
import { MatChip, MatChipRemove, MatChipSet } from '@angular/material/chips';
import { CdkTextareaAutosize } from '@angular/cdk/text-field';
import { MatCheckbox } from '@angular/material/checkbox';
import { TranslatePipe } from '@ngx-translate/core';

export class MyErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const invalidCtrl = !!(control?.invalid && control?.parent?.dirty);
    const invalidParent = !!(control?.parent?.invalid && control?.parent?.dirty);
    return control.touched && (invalidCtrl || invalidParent);
  }
}

@Component({
    selector: 'ft-envelope-mail-form',
    templateUrl: './envelope-mail-form.component.html',
    styleUrls: ['./envelope-mail-form.component.scss'],
    imports: [ReactiveFormsModule, NgClass, MatFormField, MatLabel, MatInput, MatError, MatIconButton, MatSuffix, MatTooltip, MatIcon, MatChipSet, MatChip, MatChipRemove, CdkTextareaAutosize, MatCheckbox, TranslatePipe]
})
export class EnvelopeMailFormComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private uploadManagerService = inject(UploadManagerService);
  private loginService = inject(LoginService);
  private uploadService = inject(UploadService);
  private router = inject(Router);
  private dialog = inject(MatDialog);
  private adminService = inject(AdminService);
  private configService = inject(ConfigService);


  MAIL_REGEX = new RegExp("^[a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$");

  readonly mailFormValues = input<MailInfosModel>(undefined);
  envelopeMailForm: Signal<FormGroup>;
  public readonly onFormGroupChange = output<any>();
  readonly dest = viewChild<ElementRef>('dest');
  readonly objet = viewChild<ElementRef>('objet');
  readonly message = viewChild<ElementRef>('message');


  envelopeMailFormChangeSubscription: Subscription;
  matcher = new MyErrorStateMatcher();
  destinatairesList: string[] = [];
  destListOk = false;
  senderOk = false;
  errorEmail = false;

  constructor() {
    
    effect(() => {
      const senderMail = this.loginService.senderMail();
      untracked(() => {
        const from = this.envelopeMailForm?.().get('from');
        if (senderMail && from && from.value !== senderMail) {
          from.patchValue(senderMail);
        }
      });
    });
  }

  ngOnInit(): void {
    this.initForm();
  }

  public getMailLimit(): number {
    return this.configService.getMailLimit(this.loginService.isAgent());
  }

  initForm() {

    const destinatairesInfo = this.adminService.currentDestinatairesInfo();
    if (destinatairesInfo && destinatairesInfo.destinataires && destinatairesInfo.destinataires.length > 0) {
      destinatairesInfo.destinataires.map(ed => {
        this.destinatairesList.push(ed);
      })
      this.adminService.cleanDestinatairesList();
    }

    const mailFormValues = this.mailFormValues();
    this.envelopeMailForm = formSignal(this.fb.group({
      from: [this.loginService.senderMail() || mailFormValues?.from, { validators: [Validators.required, Validators.email], asyncValidators: [QuotaAsyncValidator.createValidator(this.uploadService)], updateOn: 'blur' }],
      to: ['', { validators: [Validators.email], updateOn: 'blur' }],
      subject: [mailFormValues?.subject, { validators: [Validators.maxLength(250)] }],
      message: [mailFormValues?.message, { validators: [Validators.maxLength(2500)] }],
      cguCheck: [mailFormValues?.cguCheck, { validators: [Validators.requiredTrue] }]
    }, { asyncValidators: MailAsyncValidator.createValidator(this.uploadService, 'from', 'to', this.destinatairesList, this.loginService) }));

    this.envelopeMailFormChangeSubscription = this.envelopeMailForm().statusChanges
      .subscribe(() => {
        let mailSend = this.envelopeMailForm().get('from').value.toLowerCase().trim();
        if (mailSend != this.envelopeMailForm().get('from').value) {
          this.envelopeMailForm().get('from').setValue(mailSend, { emitEvent: true, onlySelf: false });
        }
        this.onFormGroupChange.emit({ isValid: this.envelopeMailForm().valid, values: this.envelopeMailForm().value, destinataires: this.destinatairesList })
        this.uploadManagerService.envelopeInfos.set({ type: 'mail', ...this.envelopeMailForm().value, ...this.uploadManagerService.envelopeInfos()?.parameters ? { parameters: this.uploadManagerService.envelopeInfos().parameters } : {} });
      });
    this.reloadDestinataires();
  }

  // convenience getter for easy access to form fields
  get f() { return this.envelopeMailForm().controls; }

  reloadDestinataires() {
    const mailFormValues = this.mailFormValues();
    if (mailFormValues?.to && mailFormValues?.to.length > 0) {
      Array.prototype.push.apply(this.destinatairesList, mailFormValues?.to);
      this.envelopeMailForm().get('to').setValue('');
      this.envelopeMailForm().markAllAsTouched();
      this.envelopeMailForm().markAsDirty();
      this.checkDestinatairesList();
      this.onFormGroupChange.emit({ isValid: this.envelopeMailForm().valid, values: this.envelopeMailForm().value, destinataires: this.destinatairesList })
    }
  }

  enterDest() {
    this.dest().nativeElement.focus();
  }

  enterMessage() {
    this.message().nativeElement.focus();
  }

  onBlurDestinataires() {
    this.errorEmail = false;
    let error = this.envelopeMailForm().controls['to'].errors;
    let mailDest = this.envelopeMailForm().get('to').value.toLowerCase().trim();
    this.envelopeMailForm().get('to').setValue(mailDest);
    if (this.envelopeMailForm().get('to').value && this.envelopeMailForm().get('to').hasError('email') && (this.envelopeMailForm().get('to').value.indexOf('<') >= 0 || this.envelopeMailForm().get('to').value.indexOf(' ') >= 0 || this.envelopeMailForm().get('to').value.indexOf(',') >= 0 || this.envelopeMailForm().get('to').value.indexOf(';') >= 0)) {
      this.copyListDestinataires(this.envelopeMailForm().get('to').value.toLowerCase());
    } else
      if (this.envelopeMailForm().get('to').value && !this.envelopeMailForm().get('to').hasError('email')) {
        if (!this.MAIL_REGEX.test(this.envelopeMailForm().get('to').value.toLowerCase())) {
          this.envelopeMailForm().controls['to'].setErrors({ notValid: true, email: true }, { emitEvent: true });
          this.errorEmail = true;
          return;
        } else {
          let found = this.destinatairesList.find(o => o === this.envelopeMailForm().get('to').value.toLowerCase());
          if (!found) {
            if (this.destinatairesList.length < this.configService.getMailLimit(this.loginService.isAgent())) {
              this.destinatairesList.push(this.envelopeMailForm().get('to').value.toLowerCase());
              this.envelopeMailForm().get('to').setValue('');
              if (this.envelopeMailForm().controls['to'].getError('nbLimite') && this.envelopeMailForm().controls['to'].getError('nbLimite') == true) {
                this.envelopeMailForm().controls['to'].setErrors({ 'nbLimite': false });
              }
              this.enterDest();
            } else {
              //this.envelopeMailForm().get('to').setValue('');
              this.envelopeMailForm().controls['to'].setErrors({ 'nbLimite': true });
            }
          }
        }
      } else if (this.envelopeMailForm().get('to').value != '') {
        this.envelopeMailForm().controls['to'].setErrors(error);
        if (error) {
          this.errorEmail = true;
        }
      }
    this.checkDestinatairesList();
  }

  ngOnDestroy() {
    this.envelopeMailFormChangeSubscription.unsubscribe();
  }

  checkDestinatairesList() {
    this.envelopeMailForm().markAllAsTouched();
    this.envelopeMailForm().markAsDirty();
    this.envelopeMailForm().get('from').updateValueAndValidity();
  }

  deleteDestinataire(index) {
    this.destinatairesList.splice(index, 1);
    if (this.destinatairesList.length < this.configService.getMailLimit(this.loginService.isAgent())) {
      if (this.envelopeMailForm().controls['to'].getError('nbLimite') && this.envelopeMailForm().controls['to'].getError('nbLimite') == true) {
        this.envelopeMailForm().controls['to'].setErrors({ 'nbLimite': false });
      }
    }
    this.checkDestinatairesList();
  }

  routeToInNewWindow(_route) {
    // Converts the route into a string that can be used
    // with the window.open() function
    const url = this.router.serializeUrl(
      this.router.createUrlTree([`/${_route}`])
    );

    window.open(url, '_blank');
  }


  openMailingListManager() {
    const dialogRef = this.dialog.open(MailingListManagerComponent);
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        if (result.event === 'loadMailingListFromLocalStorage') {
          if (result.data.length <= this.configService.getMailLimit(this.loginService.isAgent())) {
            if (this.envelopeMailForm().controls['to'].getError('nbLimite') && this.envelopeMailForm().controls['to'].getError('nbLimite') == true) {
              this.envelopeMailForm().controls['to'].setErrors({ 'nbLimite': false });
            }
            for (const k in result.data) {
              this.envelopeMailForm().get('to').setValue(result.data[k]);
              this.onBlurDestinataires();
            }
          } else {
            this.envelopeMailForm().get('to').setValue('');
            this.envelopeMailForm().controls['to'].setErrors({ 'nbLimite': true });
          }

        }
        if (result.event === 'loadMailingListFromFile') {
          if (result.data.length <= this.configService.getMailLimit(this.loginService.isAgent())) {
            if (this.envelopeMailForm().controls['to'].getError('nbLimite') && this.envelopeMailForm().controls['to'].getError('nbLimite') == true) {
              this.envelopeMailForm().controls['to'].setErrors({ 'nbLimite': false });
            }
            for (const k in result.data) {
              this.envelopeMailForm().get('to').setValue(result.data[k]);
              this.onBlurDestinataires();
            }
          } else {
            this.envelopeMailForm().controls['to'].setErrors({ 'nbLimite': true });
          }
          //result.event.target.value = "";


        }

      }
    });


  }


  exportDataCSV() {
    let data = this.destinatairesList;
    let csv = data.join(';');
    var blob = new Blob([csv], { type: 'text/csv' })
    saveAs(blob, "listeDestinataires.csv");
  }


  isLoggedIn() {
    return this.loginService.isLoggedIn();
  }

  copyListDestinataires(val: any) {
    if (val.indexOf("<") > 0 && val.indexOf(">") > 0) {
      let list = this.envelopeMailForm().get('to').value.split(/</);

      if (list.length <= this.configService.getMailLimit(this.loginService.isAgent())) {
        if (this.envelopeMailForm().controls['to'].getError('nbLimite') && this.envelopeMailForm().controls['to'].getError('nbLimite') == true) {
          this.envelopeMailForm().controls['to'].setErrors({ 'nbLimite': false });
        }
        list.forEach(d => {
          if (d.indexOf(">") > 0) {
            const add = d.split(/>/)[0].trim()
            if (this.MAIL_REGEX.test(add)) {
              if (!this.destinatairesList.some(e => e === add)) {
                this.destinatairesList.push(add);
              }
            }
          }
        });
        this.envelopeMailForm().get('to').setValue('');
      } else {
        this.envelopeMailForm().get('to').setValue('');
        this.envelopeMailForm().controls['to'].setErrors({ 'nbLimite': true });
      }



    } else if (val.indexOf(' ') > 0 || val.indexOf(';') || val.indexOf(',')) {
      let list = this.envelopeMailForm().get('to').value.split(/[\ ;,]+/g);
      if ((list.length + this.destinatairesList.length) < this.configService.getMailLimit(this.loginService.isAgent())) {
        if (this.envelopeMailForm().controls['to'].getError('nbLimite') && this.envelopeMailForm().controls['to'].getError('nbLimite') == true) {
          this.envelopeMailForm().controls['to'].setErrors({ 'nbLimite': false });
        }
        const mailCheck = list.every(d => {
          return this.MAIL_REGEX.test(d);
        });
        if (mailCheck) {
          list.forEach(d => {
            if (this.MAIL_REGEX.test(d)) {
              if (!this.destinatairesList.some(e => e === d)) {
                this.destinatairesList.push(d.trim());
              }
            }
          });
          this.envelopeMailForm().get('to').setValue('');

        } else {
          this.errorEmail = true;
          this.envelopeMailForm().get('to').setErrors({ notValid: true, email: true }, { emitEvent: true })
        }
      } else {
        this.envelopeMailForm().get('to').setValue('');
        this.envelopeMailForm().controls['to'].setErrors({ 'nbLimite': true });
      }
    }
  }

}

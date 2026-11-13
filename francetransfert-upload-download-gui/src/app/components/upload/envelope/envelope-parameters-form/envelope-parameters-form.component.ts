/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, OnDestroy, OnInit, inject, input, linkedSignal, output, signal, Signal } from '@angular/core';
import { formSignal } from '../../../../shared/forms/form-signal';
import { FormBuilder, FormControl, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { ParametersModel } from 'src/app/models';
import moment from 'moment';
import { UploadManagerService, UploadService } from 'src/app/services';
import { noSpecial, majChar, minChar, numChar, dateValidator, passwordValidator, sizeControl, specialChar } from 'src/app/shared/validators/forms-validator';
import { LanguageModel } from 'src/app/models';

import { LanguageSelectionService } from 'src/app/services';
import { DateAdapter } from '@angular/material/core';
import { LOCALE_ID } from '@angular/core';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { MyDateAdapter } from './my-date-adapter';
import { environment } from '../../../../../environments/environment';
import { ConfigService } from '../../../../services/config/config.service';
import { MatFormField, MatLabel, MatSuffix, MatError, MatSelect } from '@angular/material/select';
import { MatInput } from '@angular/material/input';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatDatepickerInput, MatDatepicker } from '@angular/material/datepicker';
import { NgClass } from '@angular/common';
import { MatTooltip } from '@angular/material/tooltip';
import { MatOption } from '@angular/material/autocomplete';
import { MatCheckbox } from '@angular/material/checkbox';



@Component({
    selector: 'ft-envelope-parameters-form',
    templateUrl: './envelope-parameters-form.component.html',
    styleUrls: ['./envelope-parameters-form.component.scss'],
    providers: [
        { provide: DateAdapter, useClass: MyDateAdapter }
    ],
    imports: [ReactiveFormsModule, MatFormField, MatLabel, MatInput, MatIconButton, MatSuffix, MatIcon, MatDatepickerInput, MatError, MatDatepicker, MatTooltip, MatSelect, MatOption, NgClass, MatCheckbox, TranslatePipe]
})
export class EnvelopeParametersFormComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private uploadManagerService = inject(UploadManagerService);
  private languageSelectionService = inject(LanguageSelectionService);
  translateService = inject(TranslateService);
  private uploadService = inject(UploadService);
  private _adapter = inject<DateAdapter<any>>(DateAdapter);
  private configService = inject(ConfigService);

  readonly parametersFormValues = input<ParametersModel>(undefined);
  envelopeParametersForm: Signal<FormGroup>;
  public readonly onFormGroupChange = output<any>();
  envelopeParametersFormChangeSubscription: Subscription;
  hide = true;
  passwordHelp = 'Le mot de passe doit respecter les contraintes suivantes: \n - 12 caractères minimum \n - 64 caractères maximum \n - Au moins 3 lettres minuscules \n - Au moins 3 lettres majuscules \n - Au moins 3 chiffres \n - Au moins 3 caractères spéciaux (!@#$%^&*()_-:+)';
  minDate = new Date();
  maxDate = new Date();
  readonly languageList = signal<LanguageModel[]>(undefined);
  languageSelectionSubscription: Subscription;
  currentLanguage: string;
  /** Language of the mails: follows the service's langueCourriels, and is two-way bound to the select. */
  readonly language = linkedSignal<LanguageModel>(() =>
    this.languageList()?.find(x => x.value == this.uploadService.langueCourriels()));
  langueCourriels: String;
  zipPassword: boolean = false;
  checked: boolean = false;

  constructor() {

    this.languageList.set(this.languageSelectionService.languageList);
    this._adapter.setLocale(this.translateService.currentLang());
  }



  ngOnInit(): void {
    this.initForm();
  }

  initForm() {

    let expireDate;
    const parametersFormValues = this.parametersFormValues();
    if (parametersFormValues?.expiryDays) {
      expireDate = moment().add(parametersFormValues.expiryDays, 'days').toDate();
    } else {
      expireDate = moment().add(30, 'days').toDate();
    }
    this.maxDate = moment().add(this.configService.configInfo().uploadExpiredLimit, 'days').endOf('day').toDate();

    this.envelopeParametersForm = formSignal(this.fb.group({
      expiryDays: [expireDate, [dateValidator.bind(this)]],
      password: [parametersFormValues?.password, [Validators.minLength(12), Validators.maxLength(64), passwordValidator]],
      zipPassword: [parametersFormValues?.zipPassword],
      langueCourriels: [parametersFormValues?.langueCourriels ? parametersFormValues.langueCourriels : this.languageSelectionService.selectedLanguage()],
    }));
    this.checkErrors();
    this.envelopeParametersFormChangeSubscription = this.envelopeParametersForm().valueChanges
      .subscribe(() => {
        const _expiryDays = moment().diff(this.envelopeParametersForm().get('expiryDays').value, 'days') - 1;
        this.onFormGroupChange.emit({ isValid: this.envelopeParametersForm().valid, values: { expiryDays: -_expiryDays, ...this.envelopeParametersForm().get('password').value ? { password: this.envelopeParametersForm().get('password').value } : { password: '' } } })
        this.uploadManagerService.envelopeInfos.update(infos => (
          {
            ...infos,
            parameters: {
              langueCourriels: this.envelopeParametersForm().get('langueCourriels').value,
              expiryDays: -_expiryDays,
              zipPassword: this.envelopeParametersForm().get('zipPassword').value,
              ...this.envelopeParametersForm().get('password').value ? { password: this.envelopeParametersForm().get('password').value } : { password: '' }
            }
          }));
      });
  }

  sizeControl() {
    return sizeControl(this.envelopeParametersForm().get('password').value);
  }

  minChar() {
    return minChar(this.envelopeParametersForm().get('password').value);
  }

  majChar() {
    return majChar(this.envelopeParametersForm().get('password').value);
  }

  numChar() {
    return numChar(this.envelopeParametersForm().get('password').value);
  }

  specialChar() {
    return specialChar(this.envelopeParametersForm().get('password').value);
  }

  noSpecial() {
    return noSpecial(this.envelopeParametersForm().get('password').value);
  }

  // convenience getter for easy access to form fields
  get f() { return this.envelopeParametersForm().controls; }

  ngOnDestroy() {
    this.envelopeParametersFormChangeSubscription.unsubscribe();
  }

  checkErrors() {
    if (this.f.password.errors != null) {
      this.envelopeParametersForm().get('password').setValue('');
    }
  }

  compareFunction(a: any, b: any) {
    return a.code == b.code;
  }

  public selectLanguage(value) {
    this.uploadService.setLangueCourriels(value.value);
  }


}



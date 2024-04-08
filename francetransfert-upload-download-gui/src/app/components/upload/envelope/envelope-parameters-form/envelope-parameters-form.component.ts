/*
  * Copyright (c) Ministère de la Culture (2022)
  *
  * SPDX-License-Identifier: MIT
  * License-Filename: LICENSE.txt
  */

import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { Subscription } from 'rxjs/internal/Subscription';
import { ParametersModel } from 'src/app/models';
import * as moment from 'moment';
import { UploadManagerService, UploadService } from 'src/app/services';
import { noSpecial, majChar, minChar, numChar, dateValidator, passwordValidator, sizeControl, specialChar } from 'src/app/shared/validators/forms-validator';
import { LanguageModel } from 'src/app/models';

import { LanguageSelectionService } from 'src/app/services';
import { DateAdapter} from '@angular/material/core';
import { LOCALE_ID, Inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { MyDateAdapter } from './my-date-adapter';



@Component({
  selector: 'ft-envelope-parameters-form',
  templateUrl: './envelope-parameters-form.component.html',
  styleUrls: ['./envelope-parameters-form.component.scss'],
  providers: [
    {provide: DateAdapter, useClass: MyDateAdapter}
  ],
})
export class EnvelopeParametersFormComponent implements OnInit, OnDestroy {
  @Input() parametersFormValues: ParametersModel;
  envelopeParametersForm: FormGroup;
  @Output() public onFormGroupChange = new EventEmitter<any>();
  envelopeParametersFormChangeSubscription: Subscription;
  hide = true;
  passwordHelp = 'Le mot de passe doit respecter les contraintes suivantes: \n - 12 caractères minimum \n - 64 caractères maximum \n - Au moins 3 lettres minuscules \n - Au moins 3 lettres majuscules \n - Au moins 3 chiffres \n - Au moins 3 caractères spéciaux (!@#$%^&*()_-:+)';
  minDate = new Date();
  maxDate = new Date();
  languageList: LanguageModel[];
  languageSelectionSubscription: Subscription;
  currentLanguage: string;
  language: LanguageModel;
  langueCourriels: String;
  zipPassword: boolean = false;
  checked: boolean = false;

  constructor(private fb: FormBuilder,
    private uploadManagerService: UploadManagerService,
    private languageSelectionService: LanguageSelectionService,
    public translateService: TranslateService,
    private uploadService: UploadService,
    private _adapter: DateAdapter<any>

  ) {

    this.languageList = this.languageSelectionService.languageList;
    this.uploadService.langueCourriels.subscribe(langueCourriels => {
      this.language =  this.languageList.find(x => x.value == langueCourriels);
    });
    this._adapter.setLocale(this.translateService.currentLang);
  }



  ngOnInit(): void {
    this.initForm();
  }

  initForm() {

    let expireDate;
    if (this.parametersFormValues?.expiryDays) {
      expireDate = moment().add(this.parametersFormValues.expiryDays, 'days').toDate();
    } else {
      expireDate = moment().add(30, 'days').toDate();
    }
    this.maxDate = moment().add(90, 'days').toDate();

    this.envelopeParametersForm = this.fb.group({
      expiryDays: [expireDate, [ dateValidator]],
      password: [this.parametersFormValues?.password, [Validators.minLength(12), Validators.maxLength(64), passwordValidator]],
      zipPassword: [this.parametersFormValues?.zipPassword],
      langueCourriels: [this.parametersFormValues?.langueCourriels ? this.parametersFormValues.langueCourriels : this.languageSelectionService.selectedLanguage.getValue()],
    });
    this.checkErrors();
    this.envelopeParametersFormChangeSubscription = this.envelopeParametersForm.valueChanges
      .subscribe(() => {
        const _expiryDays = moment().diff(this.envelopeParametersForm.get('expiryDays').value, 'days') - 1;
        this.onFormGroupChange.emit({ isValid: this.envelopeParametersForm.valid, values: { expiryDays: -_expiryDays, ...this.envelopeParametersForm.get('password').value ? { password: this.envelopeParametersForm.get('password').value } : { password: '' } } })
        this.uploadManagerService.envelopeInfos.next(
          {
            ...this.uploadManagerService.envelopeInfos.getValue(),
            parameters: {
              langueCourriels: this.envelopeParametersForm.get('langueCourriels').value,
              expiryDays: -_expiryDays,
              zipPassword: this.envelopeParametersForm.get('zipPassword').value,
              ...this.envelopeParametersForm.get('password').value ? { password: this.envelopeParametersForm.get('password').value } : { password: '' }
            }
          });
      });
  }

  sizeControl() {
    return sizeControl(this.envelopeParametersForm.get('password').value);
  }

  minChar() {
    return minChar(this.envelopeParametersForm.get('password').value);
  }

  majChar() {
    return majChar(this.envelopeParametersForm.get('password').value);
  }

  numChar() {
    return numChar(this.envelopeParametersForm.get('password').value);
  }

  specialChar() {
    return specialChar(this.envelopeParametersForm.get('password').value);
  }

  noSpecial() {
    return noSpecial(this.envelopeParametersForm.get('password').value);
  }

  // convenience getter for easy access to form fields
  get f() { return this.envelopeParametersForm.controls; }

  ngOnDestroy() {
    this.envelopeParametersFormChangeSubscription.unsubscribe();
  }

  checkErrors() {
    if (this.f.password.errors != null) {
      this.envelopeParametersForm.get('password').setValue('');
    }
  }

  compareFunction(a: any, b: any) {
    return a.code == b.code;
  }

  public selectLanguage(value){
    this.uploadService.setLangueCourriels(value.value);
  }


}



/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, OnDestroy, OnInit, inject, Inject, LOCALE_ID } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { LanguageModel } from 'src/app/models';
import { LanguageSelectionService, UploadService } from 'src/app/services';
import { TranslateService } from '@ngx-translate/core';
import { DateAdapter } from '@angular/material/core';
import { ActivatedRoute } from '@angular/router';
import { take, takeUntil } from 'rxjs/operators';
import { DOCUMENT } from '@angular/common';

@Component({
  selector: 'ft-language-selector',
  templateUrl: './language-selector.component.html',
  styleUrls: ['./language-selector.component.scss']
})
export class LanguageSelectorComponent implements OnInit, OnDestroy {

  private onDestroy$: Subject<void> = new Subject();
  public selectedDate: Date = new Date();
  languageSelectionSubscription: Subscription = new Subscription();
  checkValidationSubscription: Subscription = new Subscription();
  defaultLanguage: LanguageModel;
  languageList: LanguageModel[];
  selectedOption: string;
  language: string;
  checkValidation: any;


  constructor(private languageSelectionService: LanguageSelectionService,
    public translateService: TranslateService,
    private dateAdapter: DateAdapter<Date>,
    private uploadService: UploadService,
    private _activatedRoute: ActivatedRoute,
    @Inject(DOCUMENT) private document: Document,
  ) {
    //translateService.setDefaultLang("en-US")

    translateService.setDefaultLang(localStorage.getItem('language') ? localStorage.getItem('language') : "fr-FR");
    translateService.use(localStorage.getItem('language') ? localStorage.getItem('language') : "fr-FR");
    this.language = localStorage.getItem('language') ? localStorage.getItem('language') : "fr-FR"
    this.uploadService.setLangueCourriels(localStorage.getItem('language') ? localStorage.getItem('language') : "fr-FR");


  }


  ngOnInit(): void {

    this.languageSelectionSubscription = this.languageSelectionService.selectedLanguage.subscribe(lang => {
      this.defaultLanguage = lang;
    });
    this.languageList = this.languageSelectionService.languageList;

    this._activatedRoute.queryParams.pipe(takeUntil(this.onDestroy$)).subscribe((params: Array<{ string: string }>) => {
      if (params["lang"]) {
        const paramLang = this.languageList.filter(lang => {
          return lang.value == params["lang"];
        })[0];
        if (paramLang) {
          this.defaultLanguage = paramLang;
          this.language = paramLang.value;
          this.selectLanguage(paramLang.value);
        }
      }
    });

  }


  public selectLanguage(value: any) {
    this.translateService.use(value);
    this.dateAdapter.setLocale(value);
    this.checkValidationSubscription = this.uploadService.checkValidation.subscribe(checkValidation => {
      this.checkValidation = checkValidation;
    });

    if (this.checkValidation == false) {
      this.uploadService.setLangueCourriels(value);
    }
    localStorage.setItem('language', value);
    this.document.documentElement.lang = localStorage.getItem('language');
  }

  ngOnDestroy(): void {
    this.onDestroy$.next();
    this.onDestroy$.complete();
    this.languageSelectionSubscription.unsubscribe();
    this.checkValidationSubscription.unsubscribe();
  }
}

/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, DestroyRef, OnInit, inject, DOCUMENT, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LanguageModel } from 'src/app/models';
import { LanguageSelectionService, UploadService } from 'src/app/services';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { DateAdapter } from '@angular/material/core';
import { ActivatedRoute } from '@angular/router';
import { MatFormField, MatLabel, MatSelect } from '@angular/material/select';
import { MatOption } from '@angular/material/autocomplete';
import { NgClass } from '@angular/common';


@Component({
    selector: 'ft-language-selector',
    templateUrl: './language-selector.component.html',
    styleUrls: ['./language-selector.component.scss'],
    imports: [MatFormField, MatLabel, MatSelect, MatOption, NgClass, TranslatePipe]
})
export class LanguageSelectorComponent implements OnInit {
  private languageSelectionService = inject(LanguageSelectionService);
  translateService = inject(TranslateService);
  private dateAdapter = inject<DateAdapter<Date>>(DateAdapter);
  private uploadService = inject(UploadService);
  private _activatedRoute = inject(ActivatedRoute);
  private document = inject<Document>(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);

  public selectedDate: Date = new Date();
  defaultLanguage: LanguageModel;
  readonly languageList = signal<LanguageModel[]>(undefined);
  selectedOption: string;
  readonly language = signal<string>(undefined);


  constructor() {
    const translateService = this.translateService;


    translateService.setFallbackLang(localStorage.getItem('language') ? localStorage.getItem('language') : "fr-FR");
    translateService.use(localStorage.getItem('language') ? localStorage.getItem('language') : "fr-FR");
    this.language.set(localStorage.getItem('language') ? localStorage.getItem('language') : "fr-FR");
    this.uploadService.setLangueCourriels(localStorage.getItem('language') ? localStorage.getItem('language') : "fr-FR");


  }


  ngOnInit(): void {

    this.defaultLanguage = this.languageSelectionService.selectedLanguage();
    this.languageList.set(this.languageSelectionService.languageList);

    this._activatedRoute.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params: Array<{ string: string }>) => {
      if (params["lang"]) {
        const paramLang = this.languageList().filter(lang => {
          return lang.value == params["lang"];
        })[0];
        if (paramLang) {
          this.defaultLanguage = paramLang;
          this.language.set(paramLang.value);
          this.selectLanguage(paramLang.value);
        }
      }
    });

  }


  public selectLanguage(value: any) {
    this.translateService.use(value);
    this.dateAdapter.setLocale(value);
    if (!this.uploadService.checkValidation()) {
      this.uploadService.setLangueCourriels(value);
    }
    localStorage.setItem('language', value);
    this.document.documentElement.lang = localStorage.getItem('language');
  }
}

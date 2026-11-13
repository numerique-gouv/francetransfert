/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { APP_BASE_HREF } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';
import { LanguageModel } from 'src/app/models';

@Injectable({
  providedIn: 'root'
})
export class LanguageSelectionService {
  baseHref = inject(APP_BASE_HREF);


  languageList: LanguageModel[] = [];
  readonly selectedLanguage = signal<LanguageModel>({ code: '', label: '', flagUrl: '', href: '', value: '' });

  constructor() {
    const rootHref = this.baseHref.split('/');
    this.languageList = [
      { code: 'fr', label: 'Français', flagUrl: './assets/flags/flag_fr.svg', href: `${rootHref[1]}/fr/`, value: 'fr-FR' },
      { code: 'en', label: 'English', flagUrl: './assets/flags/flag_en.svg', href: `${rootHref[1]}/en/`, value: 'en-GB' },
      //{ code: 'de', label: 'Deutsch', flagUrl: './assets/flags/flag_de.svg', href: `${rootHref[1]}/de/`, value: 'de-de' },
      //{ code: 'es', label: 'Español', flagUrl: './assets/flags/flag_es.svg', href: `${rootHref[1]}/es/`, value: 'es-ES' }
    ];
    this.selectedLanguage.set(this.languageList[0]);
  }
}

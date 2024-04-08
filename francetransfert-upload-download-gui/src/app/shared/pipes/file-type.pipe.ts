/*
  * Copyright (c) Ministère de la Culture (2022)
  *
  * SPDX-License-Identifier: MIT
  * License-Filename: LICENSE.txt
  */

import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { LanguageSelectionService, UploadService } from 'src/app/services';

@Pipe({
  name: 'filetype'
})
export class FileTypePipe implements PipeTransform {
  changes: any;
  file = '';
  langueSelected: String;
  /**
   * Returns the type of the file
   * @param {string} filename
   * @returns {string}
   */
  constructor(
    private languageService: LanguageSelectionService,
    private translate: TranslateService) {


  }




  transform(filename: string = ''): string {

    this.languageService.selectedLanguage.subscribe(langueSelected => {
      this.langueSelected = langueSelected.code;
    });

    let type: string = 'Pas de type';
    if (!!filename && filename !== undefined) {

      let segments: Array<string> = filename.split('.');
      if (segments.length > 1) {
          type = ` ${segments[segments.length - 1]}`;
      }
      else {
        type = null;
      }
    }
    return type;
  }
}

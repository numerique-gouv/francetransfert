/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { Injectable } from '@angular/core';
import { MatLegacyPaginatorIntl as MatPaginatorIntl } from '@angular/material/legacy-paginator';
import { TranslateService } from '@ngx-translate/core';

@Injectable({
  providedIn: 'root'
})
export class CustomPaginatorService extends MatPaginatorIntl {

  OF_LABEL: any;

  constructor(private translate: TranslateService) {
    super();
    this.translate.onLangChange.subscribe((e: Event) => {
      this.getAndInitTranslations();
    });
    this.getAndInitTranslations();
  }

  //----------traduction----------
  getAndInitTranslations() {
    this.translate.get(['ITEMS_PER_PAGE', 'NEXT_PAGE', 'PREVIOUS_PAGE', 'OF_LABEL', 'LAST_PAGE', 'FIRST_PAGE']).subscribe(translation => {
      this.itemsPerPageLabel = translation['ITEMS_PER_PAGE'];
      this.nextPageLabel = translation['NEXT_PAGE'];
      this.previousPageLabel = translation['PREVIOUS_PAGE'];
      this.lastPageLabel = translation['LAST_PAGE'];
      this.firstPageLabel = translation['FIRST_PAGE'];
      this.OF_LABEL = translation['OF_LABEL'];
      this.changes.next();
    });
  }


  getRangeLabel = (page: number, pageSize: number, length: number) => {
    if (length === 0 || pageSize === 0) {
      return `0 ${this.OF_LABEL} ${length}`;
    }
    length = Math.max(length, 0);
    const startIndex = page * pageSize;
    const endIndex = startIndex < length ? Math.min(startIndex + pageSize, length) : startIndex + pageSize;
    return `${startIndex + 1} - ${endIndex} ${this.OF_LABEL} ${length}`;
  };
}

/* 
 * Copyright (c) Direction Interministérielle du Numérique 
 * 
 * SPDX-License-Identifier: Apache-2.0 
 * License-Filename: LICENSE.txt 
 */ 
 
/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { DatePipe } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Pipe({
  name: 'customDate',
  pure: false,
})


export class CustomDatePipe implements PipeTransform {
  constructor(private translateService: TranslateService) {}

  transform(value: Date | string, format = 'd MMMM y'): string {
    const datePipe = new DatePipe(this.translateService.currentLang || 'en');
    return datePipe.transform(value, format);
  }
}

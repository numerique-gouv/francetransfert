/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Pipe({ name: 'filetype' })
export class FileTypePipe implements PipeTransform {
  private translate = inject(TranslateService);

  changes: any;
  file = '';

  transform(filename: string = ''): string {

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

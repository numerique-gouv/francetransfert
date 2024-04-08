/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'filename'
})
export class FileNamePipe implements PipeTransform {
  /**
   * Returns the name fo the file
   * @param {string} filename
   * @returns {string}
   */
  transform(filename: string = ''): string {
    let name: string = 'Pas de nom';
    if (!!filename && filename !== undefined) {
      let segments: Array<string> = filename.split('.');
      if (!segments.length) {
        name = filename;
      } else if (segments.length === 2) {
        if (!segments[0].length) {
          name = filename;
        } else {
          name = segments[0];
        }
      } else {
        name = filename.replace(`.${segments[segments.length - 1]}`, '');
      }
    }

    return name;
  }
}

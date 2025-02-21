/* 
 * Copyright (c) Direction Interministérielle du Numérique 
 * 
 * SPDX-License-Identifier: Apache-2.0 
 * License-Filename: LICENSE.txt 
 */ 
 
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'fileunit'
})
export class FileUnitPipe implements PipeTransform {
  private units: Array<string> = ['B', 'KB', 'MB', 'GB'];


  /**
   * Returns the size of the file with unit.
   * @param {number} bytes
   * @returns {string}
   */
  transform(bytes: number): string {
    if (isNaN(parseFloat(String(bytes))) || !isFinite(bytes)) return '?';

    let unit: number = 0;
    while (bytes >= 1024) {
      bytes /= 1024;
      unit++;
    }

    return `${this.units[unit]}`;
  }
}

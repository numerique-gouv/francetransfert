import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'filesize'
})
export class FileSizePipe implements PipeTransform {


  /**
   * Returns the size of the file with unit.
   * @param {number} bytes
   * @param {number} precision
   * @returns {string}
   */
  transform(bytes: number = 0, precision: number = 2): string {
    if (isNaN(parseFloat(String(bytes))) || !isFinite(bytes)) return '?';

    while (bytes >= 1024) {
      bytes /= 1024;
    }

    return `${bytes.toFixed(+precision)}`;
  }
}

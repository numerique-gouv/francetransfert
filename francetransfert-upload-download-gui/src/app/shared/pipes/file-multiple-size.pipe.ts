/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { Pipe, PipeTransform } from '@angular/core';
import { Transfer } from '@flowjs/ngx-flow';
import { FTTransferModel } from 'src/app/models';

@Pipe({
  name: 'filemtpsize'
})
export class FileMultipleSizePipe implements PipeTransform {
  /**
   * Returns the somme of all transfers size.
   * @param {Array<FTTransfer<Transfer>>} transfers
   * @returns {number}
   */
  transform(transfers: Array<FTTransferModel<Transfer>>): number {
    if (transfers === undefined || transfers === null) {
      transfers = [];
    }
    const somme = (accumulator, currentValue) => accumulator + currentValue.size;
    return transfers.reduce(somme, 0);
  }
}

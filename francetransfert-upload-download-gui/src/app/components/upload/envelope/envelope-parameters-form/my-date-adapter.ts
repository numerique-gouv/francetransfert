/*
 * Copyright (c) Direction Interministérielle du Numérique (2023)
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.txt
 */

import { NativeDateAdapter } from "@angular/material/core";
import * as moment from "moment";


export class MyDateAdapter extends NativeDateAdapter{

  parse(value: string) {
    if((value.match(/[/]/g) || []).length == 2){
      let it=value.split('/');
      if (it.length==3)
      return new Date(+it[2],+it[1]-1,+it[0],12)
    }else{
      return  moment().add(30, 'days').toDate();
    }
    return null;
  }

  format(date: Date, displayFormat: Object) {
    return ('0'+date.getDate()).slice(-2)+'/'+('0'+(date.getMonth()+1)).slice(-2)+'/'+date.getFullYear()
  }

}

export class MyDefaultDateAdapter extends NativeDateAdapter{

  parse(value: string) {
    if (!value) {
      return null; // Retourne null pour les valeurs vides
    }

    const parts = value.split('/');
    if (parts.length === 3) {
      const day = +parts[0];
      const month = +parts[1] - 1;
      const year = +parts[2];
      return new Date(year, month, day, 12);
    } else {
      return null; // Retourne null pour les valeurs non valides
    }
  }

  format(date: Date, displayFormat: Object) {
    return ('0'+date.getDate()).slice(-2)+'/'+('0'+(date.getMonth()+1)).slice(-2)+'/'+date.getFullYear()
  }

}

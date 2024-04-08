/* 
 * Copyright (c) Ministère de la Culture (2023) 
 * 
 * SPDX-License-Identifier: MIT 
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

  }

  format(date: Date, displayFormat: Object) {
    return ('0'+date.getDate()).slice(-2)+'/'+('0'+(date.getMonth()+1)).slice(-2)+'/'+date.getFullYear()
  }

}

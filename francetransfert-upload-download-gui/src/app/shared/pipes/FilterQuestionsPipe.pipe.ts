/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'FilterQuestionsPipe',
})
export class FilterQuestionsPipe implements PipeTransform {
    transform(value: any, input: string, searchableList: any) {
        if (input) {
            input = input.toLowerCase();
            return value.filter(function (el: any) {
                var isTrue = false;
                for (var k in searchableList) {
                    if (el[searchableList[k]].toLowerCase().indexOf(input) > -1) {
                        isTrue = true;
                    }
                    if (isTrue) {
                        return el
                    }
                }
            })
        }
        return value;
    }
}

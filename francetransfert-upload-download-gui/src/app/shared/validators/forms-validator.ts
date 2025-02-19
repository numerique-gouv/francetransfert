/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { FormControl, FormGroup, ValidatorFn } from '@angular/forms';
import * as moment from 'moment';

const specialCharList = "!@#$%^&*()_-:+";
const specialCharRegexEscape = specialCharList.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, '\\$&');
const regex = new RegExp(`[${specialCharRegexEscape}]`, "g");
const notValidChar = new RegExp(`[^${specialCharRegexEscape}a-zA-Z0-9]+`, "g");

// Custom Validators
export function passwordValidator(formControl: FormControl) {
  const password = formControl.value;
  if (password) {
    if (password.match(/[a-z]/g)?.reduce((p, c) => p + c)?.length >= 3 && password.match(/[A-Z]/g)?.reduce((p, c) => p + c)?.length >= 3
      && password.match(/\d+/g)?.reduce((p, c) => p + c)?.length >= 3 && password.match(regex)?.reduce((p, c) => p + c)?.length >= 3) {
      return null;
    }
    return { pattern: true }
  } else {
    return null;
  }
}

export function dateValidator(formControl: FormControl) {
  const date = formControl.value;
  if (date > moment().add(90, 'days').toDate() || date < new Date()) {
    return { pattern: true }
  } else {
    return null;
  }
}

export function isDayNumberValid(dayNumber: number) {
  if (dayNumber > 90 || dayNumber < 1) {
    return false
  } else {
    return true;
  }
}

export function sizeControl(value: string) {
  return value?.length >= 12 && value.length <= 64;
}

export function minChar(value: string) {
  return value?.match(/[a-z]/g)?.reduce((p, c) => p + c)?.length >= 3;
}

export function majChar(value: string) {
  return value?.match(/[A-Z]/g)?.reduce((p, c) => p + c)?.length >= 3;
}

export function numChar(value: string) {
  return value?.match(/\d/g)?.reduce((p, c) => p + c)?.length >= 3;
}

export function specialChar(value: string) {
  return value?.match(regex)?.reduce((p, c) => p + c)?.length >= 3;
}

export function noSpecial(value: string) {
  return value?.match(notValidChar);
}

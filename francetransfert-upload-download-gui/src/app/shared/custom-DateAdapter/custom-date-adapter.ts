/* 
 * Copyright (c) Ministère de la Culture (2022) 
 * 
 * SPDX-License-Identifier: MIT 
 * License-Filename: LICENSE.txt 
 */ 
 
import { NativeDateAdapter } from "@angular/material/core";


/** Adapts the native JS Date for use with cdk-based components that work with dates. */
export class CustomDateAdapter extends NativeDateAdapter {
  getFirstDayOfWeek(): number {
   return 1;
  }
}

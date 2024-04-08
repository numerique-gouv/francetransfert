/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class BackgroundSelectionService {

  constructor() { }

  getBackground(): string {
    return '../assets/images/image_fond.min.png';
  }
}

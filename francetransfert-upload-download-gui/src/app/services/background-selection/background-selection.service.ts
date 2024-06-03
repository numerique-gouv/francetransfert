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
    return 'https://integration.lasuite.numerique.gouv.fr/api/backgrounds/v1/france-transfert.jpg';
  }
}

/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
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

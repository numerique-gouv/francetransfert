/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import {Component, Inject} from '@angular/core';
import {MAT_LEGACY_SNACK_BAR_DATA as MAT_SNACK_BAR_DATA} from '@angular/material/legacy-snack-bar';

@Component({
  selector: 'ft-satisfaction-message',
  templateUrl: './satisfaction-message.component.html',
  styleUrls: ['./satisfaction-message.component.scss']
})
export class SatisfactionMessageComponent {

  constructor(@Inject(MAT_SNACK_BAR_DATA) public data: string) { }


}

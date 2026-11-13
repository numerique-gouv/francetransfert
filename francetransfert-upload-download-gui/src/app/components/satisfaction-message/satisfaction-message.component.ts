/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { Component, inject } from '@angular/core';
import {MAT_SNACK_BAR_DATA} from '@angular/material/snack-bar';
import { MatIcon } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'ft-satisfaction-message',
    templateUrl: './satisfaction-message.component.html',
    styleUrls: ['./satisfaction-message.component.scss'],
    imports: [MatIcon, TranslatePipe]
})
export class SatisfactionMessageComponent {  data = inject(MAT_SNACK_BAR_DATA);



}

/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { Component, OnInit, inject } from '@angular/core';
import { MAT_SNACK_BAR_DATA } from '@angular/material/snack-bar';
import { MatIcon } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'ft-info-msg',
    templateUrl: './info-msg.component.html',
    styleUrls: ['./info-msg.component.scss'],
    imports: [MatIcon, TranslatePipe]
})
export class InfoMsgComponent implements OnInit {
  data = inject(MAT_SNACK_BAR_DATA);


  ngOnInit(): void {
  }

}

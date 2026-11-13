/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { Component, OnInit } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'ft-admin-end-msg',
    templateUrl: './admin-end-msg.component.html',
    styleUrls: ['./admin-end-msg.component.scss'],
    imports: [MatIcon, TranslatePipe]
})
export class AdminEndMsgComponent implements OnInit {

  constructor() { }

  ngOnInit(): void {
  }

}

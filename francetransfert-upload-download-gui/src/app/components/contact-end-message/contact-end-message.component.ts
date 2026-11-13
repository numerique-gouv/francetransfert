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
    selector: 'ft-contact-end-message',
    templateUrl: './contact-end-message.component.html',
    styleUrls: ['./contact-end-message.component.scss'],
    imports: [MatIcon, TranslatePipe]
})
export class ContactEndMessageComponent implements OnInit {

  constructor() { }

  ngOnInit(): void {
  }

}

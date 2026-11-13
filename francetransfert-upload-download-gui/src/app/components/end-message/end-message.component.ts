/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, OnInit, inject, input, output } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { MatCard, MatCardContent, MatCardActions } from '@angular/material/card';
import { MatButton } from '@angular/material/button';
import { CustomDatePipe } from '../../shared/pipes/custom.datepipe';

@Component({
    selector: 'ft-end-message',
    templateUrl: './end-message.component.html',
    styleUrls: ['./end-message.component.scss'],
    imports: [MatCard, MatCardContent, MatCardActions, MatButton, TranslatePipe, CustomDatePipe]
})
export class EndMessageComponent implements OnInit {
  private _router = inject(Router);
  private _translate = inject(TranslateService);


  readonly component = input<'upload' | 'download'>(undefined);
  readonly availabilityDate = input<Date>(undefined);
  readonly uploadFailed = input<boolean>(false);
  readonly backToHomeEvent = output<void>();
  readonly publicLink = input<boolean>(false);

  ngOnInit(): void {
  }

  backToHome(from: string) {
    if (from === 'upload') {
      this.backToHomeEvent.emit();
    } else {
      this._router.navigate(['/upload']);
    }
  }

  get translate(): TranslateService {
    return this._translate;
  }

}

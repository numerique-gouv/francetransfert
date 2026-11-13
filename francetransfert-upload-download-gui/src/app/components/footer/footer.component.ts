/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { Component, inject, output, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ResponsiveService } from 'src/app/services';
import { environment } from '../../../environments/environment';
import { MatToolbar, MatToolbarRow } from '@angular/material/toolbar';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'ft-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss'],
  imports: [MatToolbar, MatToolbarRow, RouterLink, TranslatePipe]
})
export class FooterComponent {
  private responsiveService = inject(ResponsiveService);
  private router = inject(Router);

  readonly isMobile = this.responsiveService.isMobile;
  readonly version = signal<string>(undefined);
  readonly routingCalled = output<boolean>();

  constructor() {
    this.version.set(environment.version);
  }

  routeTo(_route) {
    this.routingCalled.emit(true);
  }

}

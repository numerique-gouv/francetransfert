/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { Component, OnInit, inject, output } from '@angular/core';
import { Router } from '@angular/router';
import { LoginService } from 'src/app/services/login/login.service';
import { environment } from 'src/environments/environment';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { MatList, MatListItem, MatDivider } from '@angular/material/list';
import { MatIcon } from '@angular/material/icon';
import { MatLine } from '@angular/material/grid-list';
import { LanguageSelectorComponent } from '../language-selector/language-selector.component';

@Component({
    selector: 'ft-menu',
    templateUrl: './menu.component.html',
    styleUrls: ['./menu.component.scss'],
    imports: [MatList, MatListItem, MatIcon, MatLine, LanguageSelectorComponent, MatDivider, TranslatePipe]
})
export class MenuComponent implements OnInit {
  private _router = inject(Router);
  private loginService = inject(LoginService);
  private translate = inject(TranslateService);


  readonly sidenavToggle = output();
  readonly routingCalled = output<boolean>();
  version: string;

  constructor() {
    this.version = environment.version;
  }

  ngOnInit(): void {
  }

  goToLink(url) {
    this._router.navigate([url]);
    this.sidenavToggle.emit();
  }

  onToggleSidenav = () => {
    this.sidenavToggle.emit();
  }

  goToExternalLink(translationKey: string, newTab: boolean = false) {
    const link = this.translate.instant(translationKey);
    if (newTab) {
      window.open(link, '_blank');
    } else {
      window.location.href = link;
    }
  }

  isLoggedIn() {
    return this.loginService.isLoggedIn();
  }

  logout() {
    this.loginService.logout();
    this._router.navigate(['/upload']);
  }

  routeTo(_route) {
    this._router.navigate([_route]);
    this.routingCalled.emit(true);
  }

}

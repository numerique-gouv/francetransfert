/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { Router } from '@angular/router';
import { LoginService } from 'src/app/services/login/login.service';
import { environment } from 'src/environments/environment';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'ft-menu',
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.scss']
})
export class MenuComponent implements OnInit {

  @Output() sidenavToggle = new EventEmitter();
  @Output() routingCalled: EventEmitter<boolean> = new EventEmitter();
  version: string;

  constructor(private _router: Router, private loginService: LoginService, private translate: TranslateService) {
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

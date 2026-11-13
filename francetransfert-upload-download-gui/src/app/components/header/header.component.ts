import { UploadService } from './../../services/upload/upload.service';
/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, ElementRef, OnInit, ViewChild, inject, output } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ResponsiveService, UploadManagerService } from 'src/app/services';
import { LoginService } from 'src/app/services/login/login.service';
import { MatToolbar, MatToolbarRow } from '@angular/material/toolbar';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatSuffix } from '@angular/material/select';
import { MatTooltip } from '@angular/material/tooltip';
import { MatIcon } from '@angular/material/icon';
import { LanguageSelectorComponent } from '../language-selector/language-selector.component';
import { MatMenuTrigger, MatMenu, MatMenuItem } from '@angular/material/menu';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'ft-header',
    templateUrl: './header.component.html',
    styleUrls: ['./header.component.scss'],
    imports: [MatToolbar, MatToolbarRow, MatButton, RouterLink, MatSuffix, MatTooltip, MatIconButton, MatIcon, LanguageSelectorComponent, MatMenuTrigger, MatMenu, MatMenuItem, TranslatePipe]
})
export class HeaderComponent implements OnInit {
  private responsiveService = inject(ResponsiveService);
  private loginService = inject(LoginService);
  private uploadService = inject(UploadService);
  private elementRef = inject(ElementRef);
  private _router = inject(Router);

  readonly isMobile = this.responsiveService.isMobile;
  readonly sidenavToggle = output();
  readonly routingCalled = output<boolean>();
  readonly screenWidth = this.responsiveService.screenWidth;

  constructor() {
      this.uploadService.scroll();
    }

  ngOnInit(): void {
    this.elementRef.nativeElement.focus();
  }

  onToggleSidenav = () => {
    this.sidenavToggle.emit();
  }

  backToHome() {
    if (this._router.url.includes('upload')) {
      //this._router.navigate(['/upload']);
      window.location.reload();
      //this._router.navigate(['/']);

    } else {
      this._router.navigate(['/upload']);
    }
  }

  isLoggedIn() {
    return this.loginService.isLoggedIn();
  }

  logout() {
    this.loginService.logout();
    this._router.navigate(['/upload']);
  }

  goToLink(url: string) {
    window.open(url, "_blank");
  }

  routeTo(_route) {
    this._router.navigate([_route]);
    this.routingCalled.emit(true);
  }
}

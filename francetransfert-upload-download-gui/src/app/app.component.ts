/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { AfterViewInit, Component, ElementRef, OnInit, DOCUMENT, effect, inject, viewChild, signal } from '@angular/core';
import { AdminService, BackgroundSelectionService, PwaService, ResponsiveService } from './services';
import { MatDrawerMode, MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { LoaderService } from "./services/loader/loader.service";
import { MatButton } from "@angular/material/button";
import { take } from 'rxjs';
import { LoginService } from './services/login/login.service';
import { HeaderComponent } from './components/header/header.component';
import { NgStyle } from '@angular/common';
import { MenuComponent } from './components/menu/menu.component';
import { RouterOutlet } from '@angular/router';
import { FooterComponent } from './components/footer/footer.component';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  imports: [MatSidenavModule, MatButton, HeaderComponent, NgStyle, MenuComponent, RouterOutlet, FooterComponent, TranslatePipe]
})
export class AppComponent implements OnInit, AfterViewInit {
  private responsiveService = inject(ResponsiveService);
  private pwaService = inject(PwaService);
  private backgroundSelectionService = inject(BackgroundSelectionService);
  private _adminService = inject(AdminService);
  private loginService = inject(LoginService);
  private document = inject<Document>(DOCUMENT);
  loaderService = inject(LoaderService);

  readonly opened = signal<boolean>(false);
  sideNavMode: MatDrawerMode = 'over';
  readonly sidenav = viewChild.required<MatSidenav>('sidenav');
  private readonly topRoot = viewChild<ElementRef>('topRoot');
  readonly fixedTopGap = signal(114);
  readonly backgroundPath = signal<string>(undefined);
  readonly screenWidth = this.responsiveService.screenWidth;
  readonly clickHere = viewChild<MatButton>('clickHere');
  readonly buttonCancel = viewChild<ElementRef>('buttonCancel');
  readonly buttonRef = viewChild<MatButton>('btnRef');

  constructor() {
    this.pwaService.checkForUpdates();

    // Entering the mobile layout closes the side menu and drops its top gap.
    effect(() => {
      if (this.responsiveService.isMobile()) {
        this.opened.set(false);
        this.sideNavMode = 'over';
        this.fixedTopGap.set(0);
      }
    });

    // Loader overlay: focus its "cancel" / "click here" button as soon as it is rendered.
    effect(() => this.buttonRef()?.focus());
    effect(() => this.clickHere()?.focus());
  }

  ngOnInit() {
    this.backgroundPath.set(this.backgroundSelectionService.getBackground());
    this.onResize();
    this.document.documentElement.lang = localStorage.getItem('language');
  }

  onResize() {
    this.responsiveService.checkWidth();
  }

  toggleSideNav() {
    this.sidenav().toggle();
  }

  onRoutingCalled(_event) {
    const topRoot = this.topRoot();
    if (_event && topRoot) {
      topRoot.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }

  handleDownload(downloadKeyId) {
    this._adminService.getUrlExport({
      senderMail: this.loginService.tokenInfo().senderMail,
      senderToken: this.loginService.tokenInfo().senderToken
    }, downloadKeyId
    ).pipe(take(1)).subscribe(x => {
      window.location.assign(x);
    })
    this.loaderService.hide();
  }

  handleClick() {
    this.loaderService.hide();
  }

  cancelKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      this.buttonCancel().nativeElement.click();
    }
  }

  ngAfterViewInit() {

  }
}

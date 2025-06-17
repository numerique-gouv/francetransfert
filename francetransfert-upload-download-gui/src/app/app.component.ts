/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Inject,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import { Subscription } from 'rxjs/internal/Subscription';
import { AdminService, BackgroundSelectionService, PwaService, ResponsiveService, TarteaucitronService } from './services';
import { MatDrawerMode, MatSidenav } from '@angular/material/sidenav';
import { DOCUMENT } from '@angular/common';
import { LoaderService } from "./services/loader/loader.service";
import { MatButton } from "@angular/material/button";
import { take } from 'rxjs';
import { LoginService } from './services/login/login.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy, AfterViewInit {
  opened: boolean = false;
  isMobile: boolean = false;
  sideNavMode: MatDrawerMode = 'over';
  responsiveSubscription: Subscription = new Subscription;
  @ViewChild('sidenav') sidenav!: MatSidenav;
  @ViewChild('topRoot') private topRoot: ElementRef;
  fixedTopGap = 114;
  backgroundPath: string;
  screenWidth: string;
  @ViewChild('clickHere') clickHere: MatButton;
  @ViewChild('buttonCancel', { static: false }) buttonCancel: ElementRef;
  @ViewChild('btnRef') buttonRef: MatButton;
  private urlSubscription: Subscription;
  private cancelSubscription: Subscription;

  constructor(private responsiveService: ResponsiveService,
    private pwaService: PwaService,
    private tarteaucitronService: TarteaucitronService,
    private backgroundSelectionService: BackgroundSelectionService,
    private _adminService: AdminService,
    private loginService: LoginService,
    @Inject(DOCUMENT) private document: Document, public loaderService: LoaderService, private cdr: ChangeDetectorRef) {
    this.pwaService.checkForUpdates();
  }

  ngOnInit() {
    this.backgroundPath = this.backgroundSelectionService.getBackground();
    this.tarteaucitronService.initTarteaucitron();
    this.responsiveSubscription = this.responsiveService.getMobileStatus().subscribe(isMobile => {
      this.isMobile = isMobile;
      this.screenWidth = this.responsiveService.screenWidth;
      if (isMobile) {
        this.opened = false;
        this.sideNavMode = 'over';
        this.fixedTopGap = 0;
      }
    });
    this.onResize();
    this.document.documentElement.lang = localStorage.getItem('language');

    this.cancelSubscription = this.loaderService.showSpinner$.subscribe(showSpinner => {
      if (showSpinner) {
        setTimeout(() => {
          this.buttonRef.focus();
        }, 200);
      }
    });

    this.urlSubscription = this.loaderService.downloadKeyId$.subscribe(url => {
      if (url) {
        setTimeout(() => {
          if (this.clickHere) {
            this.clickHere.focus();
          }
        }, 200);
      }
    });



  }

  ngOnDestroy() {
    this.responsiveSubscription.unsubscribe();
    if (this.urlSubscription) {
      this.urlSubscription.unsubscribe();
    }
    if (this.cancelSubscription) {
      this.cancelSubscription.unsubscribe();
    }
  }

  onResize() {
    this.responsiveService.checkWidth();
  }

  toggleSideNav() {
    this.sidenav.toggle();
  }

  onRoutingCalled(_event) {
    if (_event && this.topRoot) {
      this.topRoot.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }

  handleDownload(downloadKeyId) {
    this._adminService.getUrlExport({
      senderMail: this.loginService.tokenInfo.getValue().senderMail,
      senderToken: this.loginService.tokenInfo.getValue().senderToken
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
      this.buttonCancel.nativeElement.click();
    }
  }

  ngAfterViewInit() {

  }
}

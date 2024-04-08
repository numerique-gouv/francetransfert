/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs/internal/Subscription';
import { ResponsiveService, TarteaucitronService } from 'src/app/services';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'ft-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss']
})
export class FooterComponent implements OnInit, OnDestroy {
  isMobile: boolean = false;
  responsiveSubscription: Subscription = new Subscription;
  version: string;
  @Output() routingCalled: EventEmitter<boolean> = new EventEmitter();

  constructor(private responsiveService: ResponsiveService,
    private router: Router,
    private tarteaucitronService: TarteaucitronService) {
    this.version = environment.version;
  }

  ngOnInit(): void {
    this.onResize();
    this.responsiveService.checkWidth();
  }

  ngOnDestroy() {
    this.responsiveSubscription.unsubscribe();
  }

  onResize() {
    this.responsiveSubscription = this.responsiveService.getMobileStatus().subscribe(isMobile => {
      this.isMobile = isMobile;
    });
  }

  routeTo(_route) {
    this.routingCalled.emit(true);
  }

  showTarteaucitronManager() {
    this.tarteaucitronService.showPanel();
  }

}

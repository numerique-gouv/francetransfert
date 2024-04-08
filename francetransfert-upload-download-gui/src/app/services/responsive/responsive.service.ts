/*
  * Copyright (c) Ministère de la Culture (2022)
  *
  * SPDX-License-Identifier: MIT
  * License-Filename: LICENSE.txt
  */

import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ResponsiveService {
  private isMobile = new Subject();
  public screenWidth!: string;

  constructor() {
    this.checkWidth();
  }

  onMobileChange(status: boolean) {
    this.isMobile.next(status);
  }

  getMobileStatus(): Observable<any> {
    return this.isMobile.asObservable();
  }

  public checkWidth() {
    const width = window.innerWidth;
    if (width <= 480) {
      this.screenWidth = 'sm';
      this.onMobileChange(true);
    } else if (width > 480 && width <= 1000) {
      this.screenWidth = 'md';
      this.onMobileChange(false);
    } else {
      this.screenWidth = 'lg';
      this.onMobileChange(false);
    }
  }
}

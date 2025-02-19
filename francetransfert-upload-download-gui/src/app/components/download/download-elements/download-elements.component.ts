/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Router } from "@angular/router";
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { FTErrorModel } from 'src/app/models';
import { DownloadManagerService } from 'src/app/services';
import { take } from 'rxjs/operators';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'ft-download-elements',
  templateUrl: './download-elements.component.html',
  styleUrls: ['./download-elements.component.scss']
})
export class DownloadElementsComponent implements OnInit, OnDestroy {

  @Output() dowloadStarted: EventEmitter<boolean> = new EventEmitter();
  @Input() availabilityDate: Date;
  @Input() checkRESANA: boolean;
  @Input() checkOSMOSE: boolean;
  @Input() urlResana: string;
  @Input() urlOsmose: string;
  remainingDays: number;
  checkCGU: boolean = false;
  errorDLSubscription: Subscription = new Subscription();
  checkSub: Subscription = new Subscription();
  cguForm: FormGroup;


  error: FTErrorModel;
  buttonDisable = false;
  errorMessage: string = "";

  constructor(private fb: FormBuilder, private router: Router, private downloadManagerService: DownloadManagerService,
    private translate: TranslateService,) {
    this.checkCGU = false;
    this.cguForm = this.fb.group({
      cguCheck: [false, [Validators.requiredTrue]]
    });
  }

  ngOnInit(): void {
    this.remainingDays = this.calculateDiff(this.availabilityDate);
    this.checkSub = this.cguForm.valueChanges.subscribe(cguValue => {
      this.checkCGU = cguValue.cguCheck;
    });
    this.errorDLSubscription = this.downloadManagerService.downloadError$.subscribe(error => {
      if (error) {
        this.translate.stream(error.message).pipe(take(1)).subscribe(v => {
          this.errorMessage = v;
        });
        this.error = { statusCode: error.statusCode, message: this.errorMessage, codeTryCount: error.codeTryCount };
      } else {
        this.errorMessage = "";
      }
    });
  }

  download() {
    this.dowloadStarted.emit(true);
    this.buttonDisable = true;
  }

  downloadResana() {
    window.open(this.urlResana, '_blank');
  }

  downloadOsmose() {
    window.open(this.urlOsmose, '_blank');
  }

  calculateDiff(dateSent) {
    let currentDate = new Date();
    dateSent = new Date(dateSent);
    return Math.floor((Date.UTC(dateSent.getFullYear(), dateSent.getMonth(), dateSent.getDate()) - Date.UTC(currentDate.getFullYear(), currentDate.getMonth(), currentDate.getDate())) / (1000 * 60 * 60 * 24));
  }

  routeToInNewWindow(_route) {
    // Converts the route into a string that can be used
    // with the window.open() function
    const url = this.router.serializeUrl(
      this.router.createUrlTree([`/${_route}`])
    );

    window.open(url, '_blank');
  }


  cguChecked() {
    this.checkCGU = !this.checkCGU;
  }

  ngOnDestroy(): void {
    this.checkSub.unsubscribe();
    this.errorDLSubscription.unsubscribe();
  }
}

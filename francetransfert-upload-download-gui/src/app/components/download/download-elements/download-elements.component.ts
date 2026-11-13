/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, OnDestroy, OnInit, effect, inject, input, output, signal, untracked, Signal } from '@angular/core';
import { formSignal } from '../../../shared/forms/form-signal';
import { Router } from "@angular/router";
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { FTErrorModel } from 'src/app/models';
import { DownloadManagerService } from 'src/app/services';
import { take } from 'rxjs/operators';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCard, MatCardContent, MatCardActions } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatButton } from '@angular/material/button';

@Component({
    selector: 'ft-download-elements',
    templateUrl: './download-elements.component.html',
    styleUrls: ['./download-elements.component.scss'],
    imports: [MatCard, MatCardContent, MatIcon, ReactiveFormsModule, MatCheckbox, MatCardActions, MatButton, TranslatePipe]
})
export class DownloadElementsComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private downloadManagerService = inject(DownloadManagerService);
  private translate = inject(TranslateService);


  readonly dowloadStarted = output<boolean>();
  readonly availabilityDate = input<Date>(undefined);
  readonly checkRESANA = input<boolean>(undefined);
  readonly checkOSMOSE = input<boolean>(undefined);
  readonly urlResana = input<string>(undefined);
  readonly urlOsmose = input<string>(undefined);
  readonly remainingDays = signal<number>(undefined);
  readonly checkCGU = signal<boolean>(false);
  checkSub: Subscription = new Subscription();
  cguForm: Signal<FormGroup>;


  readonly error = signal<FTErrorModel>(undefined);
  readonly buttonDisable = signal(false);
  readonly errorMessage = signal<string>("");

  constructor() {
    this.checkCGU.set(false);
    this.cguForm = formSignal(this.fb.group({
      cguCheck: [false, [Validators.requiredTrue]]
    }));

    effect(() => {
      const error = this.downloadManagerService.downloadError();
      untracked(() => {
        if (error) {
          this.translate.stream(error.message).pipe(take(1)).subscribe(v => {
            this.errorMessage.set(v);
          });
          this.error.set({ statusCode: error.statusCode, message: this.errorMessage(), codeTryCount: error.codeTryCount });
        } else {
          this.errorMessage.set("");
        }
      });
    });
  }

  ngOnInit(): void {
    this.remainingDays.set(this.calculateDiff(this.availabilityDate()));
    this.checkSub = this.cguForm().valueChanges.subscribe(cguValue => {
      this.checkCGU.set(cguValue.cguCheck);
    });
  }

  download() {
    this.dowloadStarted.emit(true);
    this.buttonDisable.set(true);
  }

  downloadResana() {
    window.open(this.urlResana(), '_blank');
  }

  downloadOsmose() {
    window.open(this.urlOsmose(), '_blank');
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
    this.checkCGU.set(!this.checkCGU());
  }

  ngOnDestroy(): void {
    this.checkSub.unsubscribe();
  }
}

/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { Component, OnDestroy, OnInit, effect, inject, input, output, untracked, Signal } from '@angular/core';
import { formSignal } from '../../../../shared/forms/form-signal';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, take } from 'rxjs/operators';
import { LinkInfosModel } from 'src/app/models';
import { UploadManagerService, UploadService } from 'src/app/services';
import { LoginService } from 'src/app/services/login/login.service';
import { MatFormField, MatLabel, MatSuffix, MatError } from '@angular/material/select';
import { MatInput } from '@angular/material/input';
import { MatIconButton } from '@angular/material/button';
import { MatTooltip } from '@angular/material/tooltip';
import { MatIcon } from '@angular/material/icon';
import { CdkTextareaAutosize } from '@angular/cdk/text-field';
import { MatCheckbox } from '@angular/material/checkbox';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'ft-envelope-link-form',
    templateUrl: './envelope-link-form.component.html',
    styleUrls: ['./envelope-link-form.component.scss'],
    imports: [ReactiveFormsModule, MatFormField, MatLabel, MatInput, MatIconButton, MatSuffix, MatTooltip, MatIcon, MatError, CdkTextareaAutosize, MatCheckbox, TranslatePipe]
})
export class EnvelopeLinkFormComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private uploadManagerService = inject(UploadManagerService);
  private uploadService = inject(UploadService);
  private loginService = inject(LoginService);
  private router = inject(Router);

  readonly linkFormValues = input<LinkInfosModel>(undefined);
  envelopeLinkForm: Signal<FormGroup>;
  public readonly onFormGroupChange = output<any>();
  envelopeLinkFormChangeSubscription: Subscription;
  envelopeLinkFormStatusSubscription: Subscription;
  senderAllowed: boolean;

  constructor() {

    effect(() => {
      const senderMail = this.loginService.senderMail();
      untracked(() => {
        const from = this.envelopeLinkForm?.().get('from');
        if (senderMail && from && from.value !== senderMail) {
          from.patchValue(senderMail);
        }
      });
    });
  }

  ngOnInit(): void {
    this.initForm();
  }

  initForm() {
    const linkFormValues = this.linkFormValues();
    this.envelopeLinkForm = formSignal(this.fb.group({
      subject: [linkFormValues?.subject],
      from: [this.loginService.senderMail() || linkFormValues?.from, { validators: [Validators.required, Validators.email], updateOn: 'blur' }],
      message: [linkFormValues?.message, { validators: [Validators.maxLength(2500)] }],
      cguCheck: [linkFormValues?.cguCheck, { validators: [Validators.requiredTrue] }]
    }));
    this.envelopeLinkFormStatusSubscription = this.envelopeLinkForm().statusChanges
      .subscribe(() => {
        let mailSend = this.envelopeLinkForm().get('from').value.toLowerCase().trim();
        if (mailSend != this.envelopeLinkForm().get('from').value) {
          this.envelopeLinkForm().get('from').setValue(mailSend, { emitEvent: true, onlySelf: false });
        }
      });
    this.envelopeLinkFormChangeSubscription = this.envelopeLinkForm().valueChanges.pipe(debounceTime(200), distinctUntilChanged())
      .subscribe(() => {
        this.checkEmitter();
        this.uploadManagerService.envelopeInfos.set({ type: 'link', ...this.envelopeLinkForm().value, ...this.uploadManagerService.envelopeInfos()?.parameters ? { parameters: this.uploadManagerService.envelopeInfos().parameters } : {} });
      });
  }

  // convenience getter for easy access to form fields
  get f() { return this.envelopeLinkForm().controls; }

  ngOnDestroy() {
    this.envelopeLinkFormChangeSubscription.unsubscribe();
    this.envelopeLinkFormStatusSubscription.unsubscribe();
  }

  checkEmitter() {
    let senderOk = false;
    this.uploadService.validateMail([this.envelopeLinkForm().get('from').value]).pipe(
      take(1)).subscribe((isValid: boolean) => {
        senderOk = isValid;
        if (this.envelopeLinkForm().get('from').value) {
          if (!senderOk) {
            this.envelopeLinkForm().controls['from'].markAsTouched();
            this.envelopeLinkForm().controls['from'].setErrors({ notValid: true });
            this.onFormGroupChange.emit({ isValid: this.envelopeLinkForm().valid, values: this.envelopeLinkForm().value })
          } else {
            this.envelopeLinkForm().controls['from'].markAsTouched();
            this.envelopeLinkForm().controls['from'].setErrors(null);
            this.uploadService.allowedSenderMail(this.envelopeLinkForm().get('from').value).pipe(take(1))
              .subscribe((isAllowed: boolean) => {
                if (!isAllowed) {
                  this.loginService.isAgent.set(false);
                  this.envelopeLinkForm().controls['from'].markAsTouched();
                  this.envelopeLinkForm().controls['from'].setErrors({ quota: true });
                } else {
                  this.loginService.isAgent.set(true);
                  this.envelopeLinkForm().controls['from'].markAsTouched();
                  this.envelopeLinkForm().controls['from'].setErrors(null);
                }
                this.onFormGroupChange.emit({ isValid: this.envelopeLinkForm().valid, values: this.envelopeLinkForm().value })
              })
          }
        } else {
          this.envelopeLinkForm().controls['from'].markAsTouched();
          this.envelopeLinkForm().controls['from'].setErrors({ required: true });
          this.onFormGroupChange.emit({ isValid: this.envelopeLinkForm().valid, values: this.envelopeLinkForm().value })
        }
      }, error => {
        this.envelopeLinkForm().controls['from'].markAsTouched();
        this.envelopeLinkForm().controls['from'].setErrors({ notValid: true });
        this.onFormGroupChange.emit({ isValid: this.envelopeLinkForm().valid, values: this.envelopeLinkForm().value })
      });

  }

  routeToInNewWindow(_route) {
    // Converts the route into a string that can be used
    // with the window.open() function
    const url = this.router.serializeUrl(
      this.router.createUrlTree([`/${_route}`])
    );

    window.open(url, '_blank');
  }

  isLoggedIn() {
    return this.loginService.isLoggedIn();
  }




}

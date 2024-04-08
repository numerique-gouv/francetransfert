/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';
import { LinkInfosModel } from 'src/app/models';
import { UploadManagerService, UploadService } from 'src/app/services';
import { LoginService } from 'src/app/services/login/login.service';

@Component({
  selector: 'ft-envelope-link-form',
  templateUrl: './envelope-link-form.component.html',
  styleUrls: ['./envelope-link-form.component.scss']
})
export class EnvelopeLinkFormComponent implements OnInit, OnDestroy {
  @Input() linkFormValues: LinkInfosModel;
  envelopeLinkForm: FormGroup;
  @Output() public onFormGroupChange = new EventEmitter<any>();
  envelopeLinkFormChangeSubscription: Subscription;
  envelopeLinkFormStatusSubscription: Subscription;
  senderAllowed: boolean;
  loginSubscription: Subscription = new Subscription();

  constructor(private fb: FormBuilder,
    private uploadManagerService: UploadManagerService,
    private uploadService: UploadService,
    private loginService: LoginService,
    private router: Router) { }

  ngOnInit(): void {
    this.initForm();
  }

  initForm() {
    this.envelopeLinkForm = this.fb.group({
      subject: [this.linkFormValues?.subject],
      from: [this.loginService.tokenInfo.getValue() && this.loginService.tokenInfo.getValue().senderMail ? this.loginService.tokenInfo.getValue().senderMail : this.linkFormValues?.from, { validators: [Validators.required, Validators.email], updateOn: 'blur' }],
      message: [this.linkFormValues?.message, { validators: [Validators.maxLength(2500)] }],
      cguCheck: [this.linkFormValues?.cguCheck, { validators: [Validators.requiredTrue] }]
    });
    this.envelopeLinkFormStatusSubscription = this.envelopeLinkForm.statusChanges
      .subscribe(() => {
        let mailSend = this.envelopeLinkForm.get('from').value.toLowerCase().trim();
        if (mailSend != this.envelopeLinkForm.get('from').value) {
          this.envelopeLinkForm.get('from').setValue(mailSend, { emitEvent: true, onlySelf: false });
        }
      });
    this.envelopeLinkFormChangeSubscription = this.envelopeLinkForm.valueChanges
      .subscribe(() => {
        this.checkEmitter();
        this.uploadManagerService.envelopeInfos.next({ type: 'link', ...this.envelopeLinkForm.value, ...this.uploadManagerService.envelopeInfos.getValue()?.parameters ? { parameters: this.uploadManagerService.envelopeInfos.getValue().parameters } : {} });
      });
    this.loginSubscription = this.loginService.loggedIn$.subscribe(loggedIn => {
      if (loggedIn) {
        this.envelopeLinkForm.get("from").patchValue(this.loginService.tokenInfo.getValue().senderMail);
      }
    });
  }

  // convenience getter for easy access to form fields
  get f() { return this.envelopeLinkForm.controls; }

  ngOnDestroy() {
    this.envelopeLinkFormChangeSubscription.unsubscribe();
    this.loginSubscription.unsubscribe();
    this.envelopeLinkFormStatusSubscription.unsubscribe();
  }

  checkEmitter() {
    let senderOk = false;
    this.uploadService.validateMail([this.envelopeLinkForm.get('from').value]).pipe(
      take(1)).subscribe((isValid: boolean) => {
        senderOk = isValid;
        if (this.envelopeLinkForm.get('from').value) {
          if (!senderOk) {
            this.envelopeLinkForm.controls['from'].markAsTouched();
            this.envelopeLinkForm.controls['from'].setErrors({ notValid: true });
            this.onFormGroupChange.emit({ isValid: this.envelopeLinkForm.valid, values: this.envelopeLinkForm.value })
          } else {
            this.envelopeLinkForm.controls['from'].markAsTouched();
            this.envelopeLinkForm.controls['from'].setErrors(null);
            this.uploadService.allowedSenderMail(this.envelopeLinkForm.get('from').value).pipe(take(1))
              .subscribe((isAllowed: boolean) => {
                if (!isAllowed) {
                  this.envelopeLinkForm.controls['from'].markAsTouched();
                  this.envelopeLinkForm.controls['from'].setErrors({ quota: true });
                } else {
                  this.envelopeLinkForm.controls['from'].markAsTouched();
                  this.envelopeLinkForm.controls['from'].setErrors(null);
                }
                this.onFormGroupChange.emit({ isValid: this.envelopeLinkForm.valid, values: this.envelopeLinkForm.value })
              })
          }
        } else {
          this.envelopeLinkForm.controls['from'].markAsTouched();
          this.envelopeLinkForm.controls['from'].setErrors({ required: true });
          this.onFormGroupChange.emit({ isValid: this.envelopeLinkForm.valid, values: this.envelopeLinkForm.value })
        }
      }, error => {
        this.envelopeLinkForm.controls['from'].markAsTouched();
        this.envelopeLinkForm.controls['from'].setErrors({ notValid: true });
        this.onFormGroupChange.emit({ isValid: this.envelopeLinkForm.valid, values: this.envelopeLinkForm.value })
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

  getSenderInfo() {
    return this.loginService.getEmail();
  }



}

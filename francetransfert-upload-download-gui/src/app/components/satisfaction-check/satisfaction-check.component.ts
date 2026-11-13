/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { Component, OnInit, inject, output, Signal } from '@angular/core';
import { formSignal } from '../../shared/forms/form-signal';
import { AbstractControl, FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCard, MatCardContent, MatCardActions } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatRadioGroup, MatRadioButton } from '@angular/material/radio';
import { MatLabel, MatSuffix, MatFormField } from '@angular/material/select';
import { NgClass } from '@angular/common';
import { MatIconButton, MatButton } from '@angular/material/button';
import { CdkTextareaAutosize } from '@angular/cdk/text-field';
import { MatInput } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'ft-satisfaction-check',
    templateUrl: './satisfaction-check.component.html',
    styleUrls: ['./satisfaction-check.component.scss'],
    imports: [MatCard, ReactiveFormsModule, MatCardContent, MatIcon, MatRadioGroup, MatLabel, MatRadioButton, NgClass, MatIconButton, MatSuffix, MatFormField, CdkTextareaAutosize, MatInput, MatCardActions, MatButton, TranslatePipe]
})
export class SatisfactionCheckComponent implements OnInit {
  private fb = inject(FormBuilder);

  satisfactionCheckForm: Signal<FormGroup>;
  selectedFace: 'dissatisfied' | 'neutral' | 'satisfied' | '';
  readonly satisfactionCheckDone = output<any>();
  messageControl: AbstractControl;

  ngOnInit(): void {
    this.initForm();
  }

  initForm() {
    this.selectedFace = '';
    this.satisfactionCheckForm = formSignal(this.fb.group({
      message: [''],
    }));
    this.messageControl = this.satisfactionCheckForm().get('message');
  }

  // convenience getter for easy access to form fields
  get f() { return this.satisfactionCheckForm().controls; }

  validateForm() {
    // stop here if form is invalid
    if (this.satisfactionCheckForm().invalid) {
      return;
    }
    let faceValue;
    switch (this.selectedFace) {
      case 'dissatisfied':
        faceValue = 1;
        break;
      case 'neutral':
        faceValue = 2;
        break;
      case 'satisfied':
        faceValue = 3;
        break;
    }
    this.satisfactionCheckDone.emit({ satisfaction: faceValue, message: this.messageControl.value });
  }

  selectFace(face) {
    this.selectedFace = face;
    if (this.selectedFace == 'satisfied') {
      this.messageControl.clearValidators();
    } else {
      this.messageControl.setValidators([Validators.required, Validators.minLength(2)]);
    }
    this.messageControl.markAsDirty();
    this.messageControl.markAsTouched();
    this.messageControl.updateValueAndValidity();
  }
}

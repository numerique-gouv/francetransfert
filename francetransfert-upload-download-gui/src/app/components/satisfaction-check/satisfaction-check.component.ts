/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'ft-satisfaction-check',
  templateUrl: './satisfaction-check.component.html',
  styleUrls: ['./satisfaction-check.component.scss']
})
export class SatisfactionCheckComponent implements OnInit {
  satisfactionCheckForm: FormGroup;
  selectedFace: 'dissatisfied' | 'neutral' | 'satisfied' | '';
  @Output() satisfactionCheckDone: EventEmitter<any> = new EventEmitter();
  messageControl: AbstractControl;

  constructor(private fb: FormBuilder) { }

  ngOnInit(): void {
    this.initForm();
  }

  initForm() {
    this.selectedFace = '';
    this.satisfactionCheckForm = this.fb.group({
      message: [''],
    });
    this.messageControl = this.satisfactionCheckForm.get('message');
  }

  // convenience getter for easy access to form fields
  get f() { return this.satisfactionCheckForm.controls; }

  validateForm() {
    // stop here if form is invalid
    if (this.satisfactionCheckForm.invalid) {
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

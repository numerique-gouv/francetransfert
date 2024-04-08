/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EnvelopeParametersFormComponent } from './envelope-parameters-form.component';

describe('EnvelopeParametersFormComponent', () => {
  let component: EnvelopeParametersFormComponent;
  let fixture: ComponentFixture<EnvelopeParametersFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ EnvelopeParametersFormComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EnvelopeParametersFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

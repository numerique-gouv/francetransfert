/*
  * Copyright (c) Ministère de la Culture (2022)
  *
  * SPDX-License-Identifier: MIT
  * License-Filename: LICENSE.txt
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EnvelopeMailFormComponent } from './envelope-mail-form.component';

describe('EnvelopeMailFormComponent', () => {
  let component: EnvelopeMailFormComponent;
  let fixture: ComponentFixture<EnvelopeMailFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ EnvelopeMailFormComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EnvelopeMailFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

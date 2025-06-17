/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
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

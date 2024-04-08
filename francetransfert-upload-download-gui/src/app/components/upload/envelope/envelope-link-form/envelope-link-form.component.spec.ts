/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EnvelopeLinkFormComponent } from './envelope-link-form.component';

describe('EnvelopeLinkFormComponent', () => {
  let component: EnvelopeLinkFormComponent;
  let fixture: ComponentFixture<EnvelopeLinkFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ EnvelopeLinkFormComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EnvelopeLinkFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

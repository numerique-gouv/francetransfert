/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PlisComponent } from './plis.component';

describe('PlisComponent', () => {
  let component: PlisComponent;
  let fixture: ComponentFixture<PlisComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PlisComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PlisComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
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

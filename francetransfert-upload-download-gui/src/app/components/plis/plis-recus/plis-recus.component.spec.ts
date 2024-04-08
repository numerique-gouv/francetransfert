/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PlisRecusComponent } from './plis-recus.component';

describe('PlisRecusComponent', () => {
  let component: PlisRecusComponent;
  let fixture: ComponentFixture<PlisRecusComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [PlisRecusComponent]
    })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PlisRecusComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

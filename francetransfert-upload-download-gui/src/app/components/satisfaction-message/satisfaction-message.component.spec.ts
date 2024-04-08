/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SatisfactionMessageComponent } from './satisfaction-message.component';

describe('SatisfactionMessageComponent', () => {
  let component: SatisfactionMessageComponent;
  let fixture: ComponentFixture<SatisfactionMessageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SatisfactionMessageComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SatisfactionMessageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

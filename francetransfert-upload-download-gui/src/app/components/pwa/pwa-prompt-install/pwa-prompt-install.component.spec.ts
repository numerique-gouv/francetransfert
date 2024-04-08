/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PwaPromptInstallComponent } from './pwa-prompt-install.component';

describe('PwaPromptInstallComponent', () => {
  let component: PwaPromptInstallComponent;
  let fixture: ComponentFixture<PwaPromptInstallComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PwaPromptInstallComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PwaPromptInstallComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

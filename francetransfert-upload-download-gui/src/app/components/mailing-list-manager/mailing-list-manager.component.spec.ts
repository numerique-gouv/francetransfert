/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MailingListManagerComponent } from './mailing-list-manager.component';

describe('MailingListManagerComponent', () => {
  let component: MailingListManagerComponent;
  let fixture: ComponentFixture<MailingListManagerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ MailingListManagerComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(MailingListManagerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

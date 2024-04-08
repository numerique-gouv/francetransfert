/* 
 * Copyright (c) Ministère de la Culture (2022) 
 * 
 * SPDX-License-Identifier: MIT 
 * License-Filename: LICENSE.txt 
 */ 
 
/*
  * Copyright (c) Ministère de la Culture (2022)
  *
  * SPDX-License-Identifier: MIT
  * License-Filename: LICENSE.txt
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DestinatairesEndMessageComponent } from './destinataires-end-message.component';

describe('DestinatairesEndMessageComponent', () => {
  let component: DestinatairesEndMessageComponent;
  let fixture: ComponentFixture<DestinatairesEndMessageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DestinatairesEndMessageComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DestinatairesEndMessageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

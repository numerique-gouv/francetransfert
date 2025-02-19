/* 
 * Copyright (c) Direction Interministérielle du Numérique 
 * 
 * SPDX-License-Identifier: Apache-2.0 
 * License-Filename: LICENSE.txt 
 */ 
 
/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
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

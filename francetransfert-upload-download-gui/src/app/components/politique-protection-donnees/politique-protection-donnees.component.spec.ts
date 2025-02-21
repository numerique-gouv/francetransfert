/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PolitiqueProtectionDonneesComponent } from './politique-protection-donnees.component';

describe('PolitiqueProtectionDonneesComponent', () => {
  let component: PolitiqueProtectionDonneesComponent;
  let fixture: ComponentFixture<PolitiqueProtectionDonneesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PolitiqueProtectionDonneesComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PolitiqueProtectionDonneesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

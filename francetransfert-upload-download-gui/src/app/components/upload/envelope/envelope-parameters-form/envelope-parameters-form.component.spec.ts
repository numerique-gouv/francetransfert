/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfigService } from 'src/app/services/config/config.service';
import { EnvelopeParametersFormComponent } from './envelope-parameters-form.component';

describe('EnvelopeParametersFormComponent', () => {
  let component: EnvelopeParametersFormComponent;
  let fixture: ComponentFixture<EnvelopeParametersFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [EnvelopeParametersFormComponent]
})
    .compileComponents();
  });

  beforeEach(() => {
    // the component reads the backend configuration (loaded at startup in the app)
    TestBed.inject(ConfigService).configInfo.set({ uploadExpiredLimit: 90 } as any);
    fixture = TestBed.createComponent(EnvelopeParametersFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

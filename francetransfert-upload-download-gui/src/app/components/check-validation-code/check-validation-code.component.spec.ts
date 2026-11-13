/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DownloadManagerService, UploadManagerService } from 'src/app/services';
import { CheckValidationCodeComponent } from './check-validation-code.component';

describe('CheckValidationCodeComponent', () => {
  let component: CheckValidationCodeComponent;
  let fixture: ComponentFixture<CheckValidationCodeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [CheckValidationCodeComponent]
})
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CheckValidationCodeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows an upload error and re-enables the validation button', () => {
    component.buttonDisable.set(true);

    TestBed.inject(UploadManagerService).uploadError.set({ statusCode: 401, message: 'INVALID_CODE', codeTryCount: 2 });
    fixture.detectChanges();

    expect(component.buttonDisable()).toBe(false);
    expect(component.error()).toMatchObject({ statusCode: 401, codeTryCount: 2 });
  });

  it('shows a download error and re-enables the validation button', () => {
    component.buttonDisable.set(true);

    TestBed.inject(DownloadManagerService).downloadError.set({ statusCode: 401, message: 'WRONG_PASSWORD', codeTryCount: 1 });
    fixture.detectChanges();

    expect(component.buttonDisable()).toBe(false);
    expect(component.error()).toMatchObject({ statusCode: 401, codeTryCount: 1 });
  });
});

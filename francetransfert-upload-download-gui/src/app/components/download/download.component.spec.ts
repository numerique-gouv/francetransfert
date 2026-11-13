/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DownloadManagerService } from 'src/app/services';
import { DownloadComponent } from './download.component';

describe('DownloadComponent', () => {
  let component: DownloadComponent;
  let fixture: ComponentFixture<DownloadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [DownloadComponent]
})
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DownloadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the error and lets the user retry after a wrong download password', async () => {
    const element: HTMLElement = fixture.nativeElement;
    component.params = { enclosure: 'pli-1', recipient: 'dest', token: 'tok' } as any;
    component.downloadInfos.set({});
    component.loading.set(false);
    await fixture.whenStable();

    const password = element.querySelector<HTMLInputElement>('input[formControlName="verificationCode"]');
    password.value = 'wrong-password';
    password.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    const validate = element.querySelector<HTMLButtonElement>('button[type="submit"]');
    expect(validate.disabled).toBe(false);
    validate.click();
    await fixture.whenStable();
    expect(validate.disabled).toBe(true);

    TestBed.inject(HttpTestingController)
      .expectOne(request => request.url.includes('validate-password'))
      .flush({ type: 'WRONG_PASSWORD', codeTryCount: 2 }, { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();

    expect(TestBed.inject(DownloadManagerService).downloadError())
      .toEqual({ statusCode: 401, message: 'WRONG_PASSWORD', codeTryCount: 2 });
    expect(element.querySelector('.check-validation-code-card-content-error [role="alert"]')).not.toBeNull();
    expect(validate.disabled).toBe(false);
  });
});

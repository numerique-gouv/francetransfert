/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FileManagerService, UploadManagerService } from 'src/app/services';
import { ConfigService } from 'src/app/services/config/config.service';
import { LoginService } from 'src/app/services/login/login.service';
import { EnvelopeComponent } from './envelope.component';

describe('EnvelopeComponent', () => {
  let component: EnvelopeComponent;
  let fixture: ComponentFixture<EnvelopeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [EnvelopeComponent]
})
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EnvelopeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('allows sending once there are files and the form of the displayed tab is valid', () => {
    const fileManager = TestBed.inject(FileManagerService);
    expect(component.canSend()).toBe(false);

    component.onMailFormGroupChangeEvent({ isValid: true, values: { type: 'mail', from: 'a@b.fr' }, destinataires: ['c@d.fr'] });
    expect(component.canSend()).toBe(false); // no file yet

    fileManager.hasFiles.set(true);
    expect(component.canSend()).toBe(true);

    component.onSelectedTabChange({ index: 1 }); // link tab: its own form has not been validated
    expect(component.canSend()).toBe(false);

    component.onLinkFormGroupChangeEvent({ isValid: true, values: { type: 'link', from: 'a@b.fr' } });
    expect(component.canSend()).toBe(true);

    fileManager.hasFiles.set(false);
    expect(component.canSend()).toBe(false);
  });

  it('writes the recipients into the values emitted by the mail form', () => {
    // The mail form publishes this same object in the envelope right after emitting.
    const values: any = { type: 'mail', from: 'a@b.fr', to: '' };

    component.onMailFormGroupChangeEvent({ isValid: true, values, destinataires: ['c@d.fr'] });

    expect(values.to).toEqual(['c@d.fr']);
    expect(component.mailFormValues()).toBe(values);
  });

  it('forces the sender of both forms on login and clears it on logout', async () => {
    const login = TestBed.inject(LoginService);

    login.tokenInfo.set({ senderMail: 'agent@gouv.fr', senderToken: 'token' } as any);
    await fixture.whenStable();
    expect(component.mailFormValues().from).toBe('agent@gouv.fr');
    expect(component.linkFormValues().from).toBe('agent@gouv.fr');

    login.tokenInfo.set(null);
    await fixture.whenStable();
    expect(component.mailFormValues().from).toBe('');
    expect(component.linkFormValues().from).toBe('');
  });

  describe('parameters', () => {
    const element = () => fixture.nativeElement as HTMLElement;
    const validateButton = () => element().querySelectorAll<HTMLButtonElement>('.envelope-card-actions button')[1];

    beforeEach(async () => {
      TestBed.inject(ConfigService).configInfo.set({ uploadExpiredLimit: 90 } as any);
      await fixture.whenStable();
      element().querySelector<HTMLButtonElement>('.envelope-card-actions button').click(); // settings
      await fixture.whenStable();
    });

    it('enables "Valider" according to the password typed', async () => {
      const password = element().querySelector<HTMLInputElement>('input[formControlName="password"]');
      const type = async (value: string) => {
        password.value = value;
        password.dispatchEvent(new Event('input'));
        await fixture.whenStable();
      };
      expect(validateButton().disabled).toBe(false);

      await type('abc');
      expect(validateButton().disabled).toBe(true);

      await type('aaaBBB111!!!xyz');
      expect(validateButton().disabled).toBe(false);
      expect(component.parametersFormValues().password).toBe('aaaBBB111!!!xyz');
    });

    it('follows the parameters published in the envelope', async () => {
      expect(validateButton().disabled).toBe(false);

      // published through the service only: no output, no DOM event
      TestBed.inject(UploadManagerService).envelopeInfos.set(
        { type: 'mail', from: 'a@b.fr', parameters: { password: 'bad', expiryDays: 30 } } as any);
      await fixture.whenStable();

      expect(validateButton().disabled).toBe(true);
    });

    it('resets an out-of-range validity to 30 days when the parameters are closed', async () => {
      // same object as the one published in the envelope, which the upload reads
      const parameters: any = { expiryDays: 500 };
      TestBed.inject(UploadManagerService).envelopeInfos.set({ type: 'mail', from: 'a@b.fr', parameters } as any);
      await fixture.whenStable();

      component.triggerShowParameters();

      expect(parameters.expiryDays).toBe(30);
      expect(component.showParameters()).toBe(false);
    });
  });
});

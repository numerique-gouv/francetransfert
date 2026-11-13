/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { TestBed } from '@angular/core/testing';
import { OAuthService } from 'angular-oauth2-oidc';

import { ConfigInfo, ConfigService } from '../config/config.service';
import { LoginService } from './login.service';

describe('LoginService', () => {
  let service: LoginService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LoginService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('starts logged out', () => {
    expect(service.tokenInfo()).toBeNull();
    expect(service.isLoggedIn()).toBe(false);
    expect(service.senderMail()).toBe('');
    expect(service.isSso()).toBe(false);
  });

  it('derives the login state from tokenInfo', () => {
    service.tokenInfo.set({ senderMail: 'agent@culture.gouv.fr', senderToken: 'token' });

    expect(service.isLoggedIn()).toBe(true);
    expect(service.senderMail()).toBe('agent@culture.gouv.fr');
    expect(service.isSso()).toBe(false);

    service.tokenInfo.set({ senderMail: 'agent@culture.gouv.fr', senderToken: 'AgentConnect', fromSso: true });
    expect(service.isSso()).toBe(true);

    service.tokenInfo.set(null);
    expect(service.isLoggedIn()).toBe(false);
    expect(service.senderMail()).toBe('');
  });

  it('setLogin only opens a session when "stay connected" was chosen', () => {
    service.setLogin({ senderMail: 'agent@culture.gouv.fr', senderToken: 'token' });
    expect(service.isLoggedIn()).toBe(false);

    service.connectCheck.set(true);
    service.setLogin({ senderMail: 'agent@culture.gouv.fr', senderToken: 'token' });
    expect(service.isLoggedIn()).toBe(true);
    expect(service.tokenInfo()).toEqual({ senderMail: 'agent@culture.gouv.fr', senderToken: 'token', fromSso: false });
  });

  it('sets up AgentConnect once, when the loaded configuration enables it', () => {
    const oauth = TestBed.inject(OAuthService);
    const configure = vi.spyOn(oauth, 'configure').mockImplementation(() => undefined);
    vi.spyOn(oauth, 'loadDiscoveryDocumentAndTryLogin').mockResolvedValue(false);
    const config = TestBed.inject(ConfigService);
    const ssoConfig = () => ({ agentConnect: true, issuerUrl: 'https://sso.test', clientId: 'ft' } as ConfigInfo);

    TestBed.tick();
    expect(configure).not.toHaveBeenCalled();

    config.configInfo.set(ssoConfig());
    TestBed.tick();
    expect(configure).toHaveBeenCalledTimes(1);
    expect(configure.mock.calls[0][0]).toMatchObject({ issuer: 'https://sso.test', clientId: 'ft' });

    // the configuration being fetched again must not configure the SSO a second time
    config.configInfo.set(ssoConfig());
    TestBed.tick();
    expect(configure).toHaveBeenCalledTimes(1);
  });
});

/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from 'src/environments/environment';

import { ConfigService } from './config.service';

describe('ConfigService', () => {
  let service: ConfigService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ConfigService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes the loaded configuration and whether AgentConnect is enabled', () => {
    expect(service.configInfo()).toBeNull();
    expect(service.isAgentConnect()).toBe(false);

    TestBed.inject(HttpTestingController)
      .expectOne(`${environment.host}${environment.apis.upload.config}`)
      .flush({ agentConnect: true, uploadMaxRecipientAgent: 100, uploadMaxRecipientPublic: 20 });

    expect(service.isAgentConnect()).toBe(true);
    expect(service.getMailLimit(true)).toBe(100);
    expect(service.getMailLimit(false)).toBe(20);
  });

  it('keeps the status of a failed configuration request until one succeeds', () => {
    const http = TestBed.inject(HttpTestingController);
    const url = `${environment.host}${environment.apis.upload.config}`;
    http.expectOne(url).flush({ agentConnect: false });

    service.getConfig().subscribe({ error: () => undefined });
    http.expectOne(url).flush('ko', { status: 503, statusText: 'Service Unavailable' });
    expect(service.configError()).toBe(503);

    service.getConfig().subscribe();
    http.expectOne(url).flush({ agentConnect: false });
    expect(service.configError()).toBeNull();
  });
});

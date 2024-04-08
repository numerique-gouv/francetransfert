/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { TestBed } from '@angular/core/testing';

import { DownloadManagerService } from './download-manager.service';

describe('DownloadManagerService', () => {
  let service: DownloadManagerService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DownloadManagerService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

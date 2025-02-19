/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
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

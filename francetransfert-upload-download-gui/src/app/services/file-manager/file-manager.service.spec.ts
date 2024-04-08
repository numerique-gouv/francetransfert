/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { TestBed } from '@angular/core/testing';

import { FileManagerService } from './file-manager.service';

describe('FileManagerService', () => {
  let service: FileManagerService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(FileManagerService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { TestBed } from '@angular/core/testing';

import { DownloadGuard } from './download.guard';

describe('DownloadGuard', () => {
  let guard: DownloadGuard;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    guard = TestBed.inject(DownloadGuard);
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });
});

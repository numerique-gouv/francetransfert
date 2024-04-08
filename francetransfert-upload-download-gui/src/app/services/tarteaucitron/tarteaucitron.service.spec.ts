/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { TestBed } from '@angular/core/testing';

import { TarteaucitronService } from './tarteaucitron.service';

describe('TarteaucitronService', () => {
  let service: TarteaucitronService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TarteaucitronService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

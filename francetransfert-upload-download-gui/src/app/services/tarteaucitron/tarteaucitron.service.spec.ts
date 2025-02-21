/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
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

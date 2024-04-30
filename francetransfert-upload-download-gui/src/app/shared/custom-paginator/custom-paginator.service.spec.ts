/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { TestBed } from '@angular/core/testing';

import { CustomPaginatorService } from './custom-paginator.service';

describe('CustomPaginatorService', () => {
  let service: CustomPaginatorService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CustomPaginatorService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

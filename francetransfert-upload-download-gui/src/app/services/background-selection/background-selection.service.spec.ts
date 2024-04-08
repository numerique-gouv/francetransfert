/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { TestBed } from '@angular/core/testing';

import { BackgroundSelectionService } from './background-selection.service';

describe('BackgroundSelectionService', () => {
  let service: BackgroundSelectionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(BackgroundSelectionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

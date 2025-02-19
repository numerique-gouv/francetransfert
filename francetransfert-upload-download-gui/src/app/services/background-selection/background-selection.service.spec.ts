/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
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

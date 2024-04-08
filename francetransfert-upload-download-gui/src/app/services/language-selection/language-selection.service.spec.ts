/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { TestBed } from '@angular/core/testing';

import { LanguageSelectionService } from './language-selection.service';

describe('LanguageSelectionService', () => {
  let service: LanguageSelectionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LanguageSelectionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

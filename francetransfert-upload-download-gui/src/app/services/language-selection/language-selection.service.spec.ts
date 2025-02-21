/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
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

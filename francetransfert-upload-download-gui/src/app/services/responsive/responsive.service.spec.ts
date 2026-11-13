/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { TestBed } from '@angular/core/testing';

import { ResponsiveService } from './responsive.service';

describe('ResponsiveService', () => {
  const initialWidth = window.innerWidth;
  const setWidth = (value: number) => Object.defineProperty(window, 'innerWidth', { value, configurable: true, writable: true });

  beforeEach(() => TestBed.configureTestingModule({}));
  afterEach(() => setWidth(initialWidth));

  it('should be created', () => {
    const service: ResponsiveService = TestBed.inject(ResponsiveService);
    expect(service).toBeTruthy();
  });

  it('derives the breakpoint and the mobile flag from the window width', () => {
    const service: ResponsiveService = TestBed.inject(ResponsiveService);

    setWidth(400);
    service.checkWidth();
    expect(service.screenWidth()).toBe('sm');
    expect(service.isMobile()).toBe(true);

    setWidth(800);
    service.checkWidth();
    expect(service.screenWidth()).toBe('md');
    expect(service.isMobile()).toBe(false);

    setWidth(1200);
    service.checkWidth();
    expect(service.screenWidth()).toBe('lg');
    expect(service.isMobile()).toBe(false);
  });
});

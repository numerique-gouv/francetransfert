/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Injectable, computed, signal } from '@angular/core';

export type ScreenWidth = 'sm' | 'md' | 'lg';

function currentScreenWidth(): ScreenWidth {
  const width = window.innerWidth;
  if (width <= 480) {
    return 'sm';
  }
  return width <= 1000 ? 'md' : 'lg';
}

@Injectable({
  providedIn: 'root'
})
export class ResponsiveService {
  readonly screenWidth = signal<ScreenWidth>(currentScreenWidth());
  readonly isMobile = computed(() => this.screenWidth() === 'sm');

  public checkWidth() {
    this.screenWidth.set(currentScreenWidth());
  }
}

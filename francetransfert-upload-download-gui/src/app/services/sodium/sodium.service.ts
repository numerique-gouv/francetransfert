/*
 * Copyright (c) Direction Interministérielle du Numérique
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.txt
 */

import { Injectable } from '@angular/core';
import _sodium from 'libsodium-wrappers';

@Injectable({
  providedIn: 'root'
})
export class SodiumService {
  private readonly readyPromise: Promise<void> = _sodium.ready;


  async getSodium(): Promise<typeof _sodium> {
    await this.readyPromise;
    return _sodium;
  }
}

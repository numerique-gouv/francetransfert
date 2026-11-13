/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl } from '@angular/forms';
import { map } from 'rxjs';


export function formSignal<T extends AbstractControl>(form: T): Signal<T> {
  return toSignal(form.events.pipe(map(() => form)), {
    initialValue: form,
    equal: () => false,
    manualCleanup: true,
  });
}

/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { AsyncValidatorFn, AbstractControl, ValidationErrors } from '@angular/forms';
import { of, Observable, from, timer } from 'rxjs';
import { catchError, map, switchMap, take } from 'rxjs/operators';
import { UploadService } from 'src/app/services';

export class QuotaAsyncValidator {
    static createValidator(uploadService: UploadService): AsyncValidatorFn {
        const ctrMethod = (control: AbstractControl): Observable<ValidationErrors> => {
            return from(uploadService.allowedSenderMail(control.value).pipe(take(1), catchError(x => {
                return of(false);
            }), map((result: boolean) => result ? null : { quota: true })));
        }
        return ctrMethod;
    }
}

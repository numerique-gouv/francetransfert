/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { AsyncValidatorFn, AbstractControl, ValidationErrors } from '@angular/forms';
import { Observable, from, forkJoin, of } from 'rxjs';
import { catchError, map, take } from 'rxjs/operators';
import { UploadService } from 'src/app/services';

export class MailAsyncValidator {
    static createValidator(uploadService: UploadService, fromField: string, toField: string, destList: string[]): AsyncValidatorFn {
        const ctrMethod = (control: AbstractControl): Observable<ValidationErrors> => {
            const fromValue = control.get(fromField).value;
            // Call validation for sender mail and list dest
            return forkJoin(
                {
                    destListOk: uploadService.validateMail(destList),
                    senderOk: uploadService.validateMail([fromValue])
                }
            ).pipe(take(1), catchError(x => {
                // if error return false for both
                return of({
                    destListOk: false,
                    senderOk: false
                });
            }),
                map(ret => {
                    let toError = control.get(toField).errors;
                    const destListOk = ret.destListOk;
                    const senderOk = ret.senderOk;
                    if (destList.length > 0) {
                        // if dest list is not empty and sender and dest ok no error
                        if (destListOk || senderOk) {
                            if (toError) {
                                delete toError['notValid'];
                                delete toError['required'];
                                if (Object.keys(toError).length === 0) {
                                    control.get(toField).setErrors(null);
                                } else {
                                    control.get(toField).setErrors(toError);
                                }
                            }
                            return null;
                        } else {
                            if (toError == null || toError == undefined) {
                                toError = {};
                            }
                            toError['notValid'] = true;
                            control.get(toField).setErrors(toError);
                            return { notValid: true };
                        }
                    } else {
                        // If no dest set required error
                        if (toError == null || toError == undefined) {
                            toError = {};
                        }
                        toError['required'] = true;
                        control.get(toField).setErrors(toError);
                        return { required: true };
                    }
                }));
        }
        return ctrMethod;
    }
}

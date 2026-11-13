/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { v4 as uuidv4 } from 'uuid';
import { SessionsService } from '../../services/sessions/sessions.service';

const CORRELATION_ID_HEADER = 'x-correlation-id';
const SESSION_ID_HEADER = 'x-session-id';

export const correlationIdInterceptor: HttpInterceptorFn = (req, next) => {
  const sessionsService = inject(SessionsService);
  let requestToHandle = req;

  if (!requestToHandle.headers.has(CORRELATION_ID_HEADER)) {
    requestToHandle = requestToHandle.clone({
      headers: requestToHandle.headers.set(CORRELATION_ID_HEADER, uuidv4()),
    });
  }

  if (!requestToHandle.headers.has(SESSION_ID_HEADER)) {
    requestToHandle = requestToHandle.clone({
      headers: requestToHandle.headers.set(SESSION_ID_HEADER, sessionsService.sessionId),
    });
  }

  return next(requestToHandle);
};

/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.exception;

public class RetryGlimpsException extends RuntimeException {

    public RetryGlimpsException(String message) {
        super(message);
    }

    public RetryGlimpsException(Throwable ex) {
        super(ex);
    }

    public RetryGlimpsException(String message, Throwable ex) {
        super(message, ex);
    }

}
/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.domain.exceptions;

public class MaxTryException extends RuntimeException {
    /**
     * Unauthorized Access Exception
     * @param msg
     */
    public MaxTryException(String msg){
        super(msg);
    }
}

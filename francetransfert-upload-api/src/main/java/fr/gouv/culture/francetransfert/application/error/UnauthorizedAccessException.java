/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.error;

/**
 * Exception used in access treatment
 *
 */
public class UnauthorizedAccessException extends RuntimeException  {


    /**
     * Unauthorized Access Exception
     * @param msg
     */
    public UnauthorizedAccessException(String msg){
        super(msg);
    }
}

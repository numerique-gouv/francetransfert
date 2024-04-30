/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.domain.exceptions;

public class InvalidCaptchaException extends RuntimeException {
    /**
     * Unauthorized Access Exception
     * @param msg
     */
	public InvalidCaptchaException(String msg){
        super(msg);
    }
}

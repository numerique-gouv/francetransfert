/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.domain.exceptions;

public class InvalidHashException extends RuntimeException {

	/**
	 * throw business domain exception
	 * 
	 * @param message
	 */
	public InvalidHashException(String message) {
		super(message);
	}

	public InvalidHashException(String message, Throwable ex) {
		super(message, ex);
	}
}

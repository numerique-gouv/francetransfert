/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.exception;

public class RetryException extends Exception {

	public RetryException(String message) {
		super(message);
	}

	public RetryException(Throwable ex) {
		super(ex);
	}

	public RetryException(String message, Throwable ex) {
		super(message, ex);
	}

}
/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.exception;

public class StatException extends Exception {

	public StatException(String message) {
		super(message);
	}

	public StatException(Throwable ex) {
		super(ex);
	}

	public StatException(String message, Throwable ex) {
		super(message, ex);
	}

}

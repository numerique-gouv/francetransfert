/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.exception;

public class ClamAVException extends Exception {

	public ClamAVException(String message) {
		super(message);
	}

	public ClamAVException(String message, Throwable cause) {
		super(message, cause);
	}
}

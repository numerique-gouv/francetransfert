/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.exception;

public class RetryStorageException extends Exception {

	public RetryStorageException(String message) {
		super(message);
	}

	public RetryStorageException(Throwable ex) {
		super(ex);
	}

	public RetryStorageException(String message, Throwable ex) {
		super(message, ex);
	}

}
/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.exception;

public class StorageException extends Exception {

	public StorageException(String message) {
		super(message);
	}

	public StorageException(Throwable ex) {
		super(ex);
	}

	public StorageException(String message, Throwable ex) {
		super(message, ex);
	}

}
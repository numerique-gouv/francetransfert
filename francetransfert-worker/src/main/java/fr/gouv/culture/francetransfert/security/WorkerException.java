/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.security;

public class WorkerException extends RuntimeException {

	public WorkerException(String message) {
		super(message);
	}

	public WorkerException(Throwable ex) {
		super(ex);
	}

	public WorkerException(String message, Throwable ex) {
		super(message, ex);
	}

}
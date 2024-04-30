/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.domain.exceptions;

public class DomainNotFoundException extends RuntimeException {

	/**
	 * Domain not found Exception
	 * 
	 * @param domain domain class example Parameter
	 * @param id     id of the domain
	 */
	public DomainNotFoundException(String message, Throwable ex) {
		super(message, ex);
	}

}

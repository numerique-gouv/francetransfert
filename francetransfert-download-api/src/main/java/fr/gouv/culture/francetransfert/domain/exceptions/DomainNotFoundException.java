/*
  * Copyright (c) Direction Interministérielle du Numérique 
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
	public DomainNotFoundException(Class domain, Long id) {
		super(domain.getSimpleName() + " id not found : " + id);
	}

}

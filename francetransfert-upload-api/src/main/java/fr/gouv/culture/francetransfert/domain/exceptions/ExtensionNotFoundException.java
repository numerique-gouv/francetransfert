/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.domain.exceptions;

public class ExtensionNotFoundException extends RuntimeException {

	public ExtensionNotFoundException(String extension) {
		super(extension);
	}

}

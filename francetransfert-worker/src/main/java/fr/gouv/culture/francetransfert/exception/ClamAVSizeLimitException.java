/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.exception;

/**
 * Exception générique pendant une opération de la SCAN API.
 */
public class ClamAVSizeLimitException extends RuntimeException {

	/**
	 * {@inheritDoc}
	 * 
	 * @param message Description spécifique
	 */
	public ClamAVSizeLimitException(String message) {
		super(message);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param message Description spécifique
	 * @param e       Exception parente
	 */
	public ClamAVSizeLimitException(String message, Throwable e) {
		super(message, e);
	}
}

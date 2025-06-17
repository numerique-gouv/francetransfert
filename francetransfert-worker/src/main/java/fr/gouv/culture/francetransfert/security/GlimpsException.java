/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.security;

public class GlimpsException extends RuntimeException {

	public GlimpsException(String message) {
		super(message);
	}

	public GlimpsException(Throwable ex) {
		super(ex);
	}

	public GlimpsException(String message, Throwable ex) {
		super(message, ex);
	}

}
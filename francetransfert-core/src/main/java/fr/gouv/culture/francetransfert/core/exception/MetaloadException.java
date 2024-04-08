/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.exception;

import lombok.Getter;

/**
 * Exception générique pendant une opération de la Metaload API.
 */
@Getter
public class MetaloadException extends Exception {

	/**
	 * Serialization id généré.
	 */
	private static final long serialVersionUID = 5657417082244325824L;

	private String type;
	private String id;
	private Throwable ex;

	public MetaloadException(String type, String id) {
		super(type);
		this.type = type;
		this.id = id;
	}

	public MetaloadException(String message) {
		super(message);
		this.type = message;
	}

	public MetaloadException(String message, Throwable ex) {
		super(message, ex);
		this.type = message;
	}

	public MetaloadException(String message, String id, Throwable ex) {
		super(message, ex);
		this.id = id;
		this.type = message;
	}

	public MetaloadException(Throwable ex) {
		super(ex);
		this.type = ex.getMessage();
	}

}

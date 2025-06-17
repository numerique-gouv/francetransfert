/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.domain.exceptions;

import lombok.Getter;

@Getter
public class UploadException extends RuntimeException {
	private String type;
	private String id;
	private Throwable ex;

	public UploadException(String type, String id) {
		super(type);
		this.type = type;
		this.id = id;
	}

	public UploadException(String message) {
		super(message);
		this.type = message;
	}

	public UploadException(String message, Throwable ex) {
		super(message, ex);
		this.type = message;
	}

	public UploadException(String message, String id, Throwable ex) {
		super(message, ex);
		this.id = id;
		this.type = message;
	}

	public UploadException(Throwable ex) {
		super(ex);
		this.type = ex.getMessage();
	}

}

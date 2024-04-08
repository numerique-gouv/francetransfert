/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.domain.exceptions;

import lombok.Data;

@Data
public class DownloadException extends RuntimeException {
	private String id;

	public DownloadException(String type, String id, Throwable ex) {
		super(type, ex);
		this.id = id;
	}

	public DownloadException(String type, String id) {
		super(type);
		this.id = id;
	}

}

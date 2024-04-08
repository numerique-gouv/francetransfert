/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.domain.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class ConfirmationCodeException extends RuntimeException {
	private String type;
	private int count;

	public ConfirmationCodeException(String type, int count) {
		super(type);
		this.type = type;
		this.count = count;
	}

}

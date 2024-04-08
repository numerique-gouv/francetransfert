/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorEnum {
	TECHNICAL_ERROR("TECHNICAL_ERROR"), DOWNLOAD_LIMIT("DOWNLOAD_LIMIT"), DELETED_ENCLOSURE("DELETED_ENCLOSURE"),
	WRONG_PASSWORD("WRONG_PASSWORD"), USER_DELETED("USER_DELETED"), MAX_TRY("MAX_TRY"), HASH_INVALID("HASH_INVALID"),
	WRONG_ENCLOSURE("WRONG_ENCLOSURE");

	private String value;
}

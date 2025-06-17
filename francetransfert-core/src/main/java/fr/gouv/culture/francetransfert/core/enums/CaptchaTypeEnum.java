/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.enums;

public enum CaptchaTypeEnum {
	SOUND("SOUND"), IMAGE("IMAGE");

	private String value;

	CaptchaTypeEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}

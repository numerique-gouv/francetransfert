/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.enums;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum PliKeysEnum {
	ENCLOSURE("enclosure");

	private String key;

	PliKeysEnum(String key) {
		this.key = key;
	}

	public static List<String> keys() {
		return Stream.of(PliKeysEnum.values()).map(e -> e.key).collect(Collectors.toList());
	}

	public String getKey() {
		return key;
	}
}

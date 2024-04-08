/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.enums;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum SenderKeysEnum {
	EMAIL("email"), IS_NEW("is-new"), ID("id");

	private String key;

	SenderKeysEnum(String key) {
		this.key = key;
	}

	public static List<String> keys() {
		return Stream.of(SenderKeysEnum.values()).map(e -> e.key).collect(Collectors.toList());
	}

	public String getKey() {
		return key;
	}
}

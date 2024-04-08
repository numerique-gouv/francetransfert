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

public enum RecipientKeysEnum {
	NB_DL("nb-dl"), PASSWORD_TRY_COUNT("password-try-count"), LAST_PASSWORD_TRY("password-try-timestamp"),
	LOGIC_DELETE("logic-delete");

	private String key;

	RecipientKeysEnum(String key) {
		this.key = key;
	}

	public static List<String> keys() {
		return Stream.of(RecipientKeysEnum.values()).map(e -> e.key).collect(Collectors.toList());
	}

	public String getKey() {
		return key;
	}
}

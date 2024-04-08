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

public enum EnclosureKeysEnum {
	TIMESTAMP("timestamp"), EXPIRED_TIMESTAMP("expired-timestamp"), PASSWORD("password"),
	PASSWORD_GENERATED("password-generated"), MESSAGE("message"), SUBJECT("subject"),
	UPLOAD_NB_FILES_DONE("upload-nb-files-done"), PUBLIC_LINK("public-link"),
	PUBLIC_DOWNLOAD_COUNT("public-download-count"), TOKEN("token"), HASH_FILE("hash-file"), LANGUAGE("language"),
	PASSWORD_ZIP("zip-password"), DELETED("deleted"), EXPIRED_TIMESTAMP_ARCHIVE("expired-timestamp-archive"),
	STATUS_CODE("status-code"), STATUS_WORD("status-word"), SOURCE("source"), INFOPLI("info-pli"), ENVOIMDPDEST("envoiMdpDestinataires");

	private String key;

	EnclosureKeysEnum(String key) {
		this.key = key;
	}

	public static List<String> keys() {
		return Stream.of(EnclosureKeysEnum.values()).map(e -> e.key).collect(Collectors.toList());
	}

	public String getKey() {
		return key;
	}
}

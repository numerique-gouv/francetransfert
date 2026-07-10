/*
 * Copyright (c) Direction Interministerielle du Numerique
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.txt
 */

package fr.gouv.culture.francetransfert.core.utils;

public final class MdcKeys {

	public static final String CORRELATION_ID_HEADER = "x-correlation-id";
	public static final String SESSION_ID_HEADER = "x-session-id";
	public static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
	public static final String REAL_IP_HEADER = "X-Real-IP";

	public static final String CORRELATION_ID = "correlationId";
	public static final String SESSION_ID = "sessionId";
	public static final String CALLER_IP = "callerIp";
	public static final String ENCLOSURE_ID = "enclosureId";
	public static final String RECIPIENT = "actingUser";
	public static final String CONNECTED_USER = "connectedUser";
	public static final String FILE_NAME = "fileName";
	public static final String FILE_HASH = "fileHash";
	public static final String FILE_MIME_TYPE = "fileMimeType";
	public static final String FILE_EXTENSION = "fileExtension";

	private MdcKeys() {
	}
}

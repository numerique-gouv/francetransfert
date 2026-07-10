/*
 * Copyright (c) Direction Interministerielle du Numerique
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.txt
 */

package fr.gouv.culture.francetransfert.core.utils;

public final class MdcValueSanitizer {

	private static final int DEFAULT_MAX_VALUE_LENGTH = 256;

	private MdcValueSanitizer() {
	}

	public static String sanitize(String value) {
		return sanitize(value, DEFAULT_MAX_VALUE_LENGTH);
	}

	public static String sanitize(String value, int maxValueLength) {
		if (value == null) {
			return null;
		}
		String cleaned = value.trim().replaceAll("[\\r\\n\\t]", " ");
		if (cleaned.isEmpty()) {
			return null;
		}
		if (maxValueLength > 0 && cleaned.length() > maxValueLength) {
			cleaned = cleaned.substring(0, maxValueLength);
		}
		return cleaned;
	}
}

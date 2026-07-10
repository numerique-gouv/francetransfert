/*
 * Copyright (c) Direction Interministerielle du Numerique
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.txt
 */

package fr.gouv.culture.francetransfert.core.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.MDC;

public final class MdcScope implements AutoCloseable {

	private final Map<String, String> previousValues = new HashMap<>();

	public static MdcScope enclosure(String enclosureId) {
		return new MdcScope().put(MdcKeys.ENCLOSURE_ID, enclosureId);
	}

	public static MdcScope file(String fileName, String fileHash) {
		return file(fileName, fileHash, null);
	}

	public static MdcScope file(String fileName, String fileHash, String mimeType) {
		return new MdcScope().put(MdcKeys.FILE_NAME, FilenameUtils.getName(fileName)).put(MdcKeys.FILE_HASH, fileHash)
				.put(MdcKeys.FILE_MIME_TYPE, mimeType).put(MdcKeys.FILE_EXTENSION, FilenameUtils.getExtension(fileName));
	}

	public static MdcScope context(Map<String, String> contextMap) {
		MdcScope scope = new MdcScope();
		if (contextMap != null) {
			contextMap.forEach(scope::put);
		}
		return scope;
	}

	public MdcScope put(String key, String value) {
		String sanitized = MdcValueSanitizer.sanitize(value);
		if (sanitized == null || previousValues.containsKey(key)) {
			return this;
		}
		previousValues.put(key, MDC.get(key));
		MDC.put(key, sanitized);
		return this;
	}

	@Override
	public void close() {
		previousValues.forEach((key, previousValue) -> {
			if (previousValue != null) {
				MDC.put(key, previousValue);
			} else {
				MDC.remove(key);
			}
		});
		previousValues.clear();
	}
}

/*
 * Copyright (c) Direction Interministerielle du Numerique
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.txt
 */

package fr.gouv.culture.francetransfert.core.utils;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CorrelationIdResolver {

	private static final Pattern CALLER_IP_PATTERN = Pattern.compile("^[A-Fa-f0-9:._%-]{1,128}$");

	public String resolveCorrelationId(String correlationIdHeader) {
		return StringUtils.hasText(correlationIdHeader) ? correlationIdHeader.trim() : UUID.randomUUID().toString();
	}

	public String resolveSessionId(String sessionIdHeader) {
		return StringUtils.hasText(sessionIdHeader) ? sessionIdHeader.trim() : UUID.randomUUID().toString();
	}

	public String resolveCallerIp(List<String> forwardedForValues, String remoteAddr) {
		if (forwardedForValues != null) {
			for (String value : forwardedForValues) {
				if (StringUtils.hasText(value)) {
					return sanitizeCallerIp(value.split(",")[0].trim(), remoteAddr);
				}
			}
		}
		return sanitizeCallerIp(remoteAddr, "-");
	}

	private String sanitizeCallerIp(String candidate, String fallback) {
		if (StringUtils.hasText(candidate) && CALLER_IP_PATTERN.matcher(candidate).matches()) {
			return candidate;
		}
		if (StringUtils.hasText(fallback) && CALLER_IP_PATTERN.matcher(fallback).matches()) {
			return fallback;
		}
		return "-";
	}
}

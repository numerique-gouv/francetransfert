/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

package fr.gouv.culture.francetransfert.core.utils;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LanguageUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(LanguageUtils.class);

	public static final String LANGUAGE_REGEX = "(?i)^(fr([-_]fr)?|en([-_]gb)?)$";

	public static final String FRENCH = "fr-FR";
	public static final String ENGLISH = "en-GB";

	private LanguageUtils() {
	}

	public static boolean isValidLanguage(String language) {
		return language != null && language.matches(LANGUAGE_REGEX);
	}

	public static String normalizeLanguage(String language) {
		if (!isValidLanguage(language)) {
			return FRENCH;
		}
		return language.toLowerCase(Locale.ROOT).startsWith("en") ? ENGLISH : FRENCH;
	}

	public static Locale toSupportedLocale(String language) {
		if (StringUtils.isNotBlank(language)) {
			Locale locale = Locale.forLanguageTag(language.trim().replace('_', '-'));
			if (Locale.UK.getLanguage().equals(locale.getLanguage())) {
				return Locale.UK;
			}
		}
		return Locale.FRANCE;
	}
}

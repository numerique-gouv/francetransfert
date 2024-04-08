/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.utils;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/**
 * Custom static utility methods for using the Amazon S3 API.
 */
public final class AmazonS3Utils {

	private AmazonS3Utils() {
	}

	/**
	 * <p>
	 * Association of problematic characters (for Amazon S3 API calls) with their
	 * replacements (very unlikely character sequences). Using the Java 8 Stream
	 * API, therefore each key / value pair for the Map is given as a String pair.
	 * </p>
	 * 
	 * <p>
	 * <strong>Warning:</strong> do not change the replacements unless certain that
	 * there isn't a single object in the target Amazon S3 storage with replacement
	 * requences in its key.
	 * </p>
	 * 
	 * @see #escapeProblemCharsForObjectKey(String)
	 * @see #unescapeProblemCharsForObjectKey(String)
	 */
	private static final Map<String, String> _PROBLEM_CHARS_FOR_KEY_ESCAPES = Stream.of(new String[][] {
			{ "$", "]]0[[" }, { "&", "]]1[[" }, { "@", "]]2[[" }, { "+", "]]3[[" }, { "=", "]]4[[" }, { "!", "]]5[[" },
			{ ";", "]]6[[" }, { ",", "]]7[[" }, { "'", "]]8[[" }, { "(", "]]9[[" }, { ")", "]]A[[" } })
			.collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));

	/**
	 * <p>
	 * Keys of {@link #_PROBLEM_CHARS_FOR_KEY_ESCAPES}, i.e. array of problematic
	 * characters.
	 * </p>
	 * <p>
	 * See the static initializer.
	 * </p>
	 */
	private static final String[] _PROBLEM_CHARS_FOR_KEY_PBCHARS;
	/**
	 * <p>
	 * Values of {@link #_PROBLEM_CHARS_FOR_KEY_ESCAPES}, i.e. array of replacement
	 * sequences for the problematic characters, at positions corresponding exactly
	 * to those of {@link #_PROBLEM_CHARS_FOR_KEY_PBCHARS}.
	 * </p>
	 * <p>
	 * See the static initializer.
	 * </p>
	 */
	private static final String[] _PROBLEM_CHARS_FOR_KEY_ESCSEQS;

	static {

		// Convert the PROBLEM_CHARS_FOR_KEY_ESCAPES map to zrrays for use with
		// commons-lang3.StringUtils.replaceEach()

		int n = _PROBLEM_CHARS_FOR_KEY_ESCAPES.size();
		_PROBLEM_CHARS_FOR_KEY_PBCHARS = new String[n];
		_PROBLEM_CHARS_FOR_KEY_ESCSEQS = new String[n];

		int i = 0;
		for (Map.Entry<String, String> entry : _PROBLEM_CHARS_FOR_KEY_ESCAPES.entrySet()) {
			_PROBLEM_CHARS_FOR_KEY_PBCHARS[i] = entry.getKey();
			_PROBLEM_CHARS_FOR_KEY_ESCSEQS[i] = entry.getValue();
			i++;
		}
	}

	/**
	 * <p>
	 * In the key of an Amazon S3 object, replaces all characters that may cause
	 * problems with very unlikely sequences of characters. See
	 * <a href="https://jira.open-groupe.com/browse/FT-379">FT-379</a>: calls to the
	 * Amazon S3 API fail when the request contains certain characters, that may or
	 * may not all be HTTP-sensitive.
	 * </p>
	 * 
	 * <p>
	 * No need to escape bucket names since they are already bound by DNS naming
	 * rules.
	 * </p>
	 *
	 * @param objectKey The key of an S3 object, that is, its storage pathname
	 *                  within a bucket
	 * @return The object key provided, with problematic characters replaced by very
	 *         unlikely character sequences
	 */
	public static String escapeProblemCharsForObjectKey(String objectKey) {

		return StringUtils.replaceEach(objectKey, _PROBLEM_CHARS_FOR_KEY_PBCHARS, _PROBLEM_CHARS_FOR_KEY_ESCSEQS);
	}

	/**
	 * Reverse of {@link #escapeProblemCharsForObjectKey(String)}.
	 * 
	 * @param escapedObjectKey The key of an S3 object, that has been escaped with
	 *                         the converse method
	 * @return The original key for the object, with problematic characters restored
	 */
	public static String unescapeProblemCharsForObjectKey(String escapedObjectKey) {

		return StringUtils.replaceEach(escapedObjectKey, _PROBLEM_CHARS_FOR_KEY_ESCSEQS,
				_PROBLEM_CHARS_FOR_KEY_PBCHARS);
	}
}

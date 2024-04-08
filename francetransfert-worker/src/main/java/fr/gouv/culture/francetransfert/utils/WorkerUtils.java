/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import fr.gouv.culture.francetransfert.security.WorkerException;

public class WorkerUtils {

	public static final List<MonitorRunnable> activeTasks = Collections.synchronizedList(new ArrayList<>());

	private WorkerUtils() {
		// private Constructor
	}

	public static String infinity_Date = "2000-12-31";

	public static LocalDateTime convertStringToLocalDateTime(String date) {
		if (null == date) {
			date = infinity_Date;
		}
		// convert String to LocalDateTime
		return LocalDateTime.parse(date);
	}

	public static String convertStringToLocalDateTime(LocalDateTime localDateTime) {
		String result = "";
		if (localDateTime != null) {
			result = localDateTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH));
		}
		return result;
	}

	public static String extractEnclosureIdFromDownloadQueueValue(String downloadQueueValue) {
		return extractPartOfString(0, downloadQueueValue);
	}

	public static String extractRecipientIdFromDownloadQueueValue(String downloadQueueValue) {
		return extractPartOfString(1, downloadQueueValue);
	}

	public static String extractPartOfString(int part, String string) {
		Pattern pattern = Pattern.compile(":");
		String[] items = pattern.split(string, 2);
		if (2 == items.length) {
			return items[part];
		} else {
			throw new WorkerException("error of extraction value");
		}
	}

	public static String getFormatFileSizeLanguage(Locale language, double size) {
		if (Locale.UK.equals(language)) {
			return getFormattedFileSizeEn(size);
		} else {
			return getFormattedFileSize(size);
		}
	}

	// convert bit to octets", "Ko", "Mo", "Go", "To
	public static String getFormattedFileSize(double size) {
		String[] suffixes = new String[] { "octets", "Ko", "Mo", "Go", "To" };

		double tmpSize = size;
		int i = 0;

		while (tmpSize >= 1024) {
			tmpSize /= 1024.0;
			i++;
		}

		// arrondi à 10^-2
		tmpSize *= 100;
		tmpSize = (long) (tmpSize + 0.5);
		tmpSize /= 100;

		return tmpSize + " " + suffixes[i];
	}

	// convert bit to octets", "Ko", "Mo", "Go", "To
	public static String getFormattedFileSizeEn(double size) {
		String[] suffixes = new String[] { "B", "KB", "MB", "GB", "TB" };

		double tmpSize = size;
		int i = 0;

		while (tmpSize >= 1024) {
			tmpSize /= 1024.0;
			i++;
		}

		// arrondi à 10^-2
		tmpSize *= 100;
		tmpSize = (long) (tmpSize + 0.5);
		tmpSize /= 100;

		return tmpSize + " " + suffixes[i];
	}

	public static boolean isValidRegex(String p, String str) {
		if (null == str) {
			return false;
		}
		return Pattern.matches(p, str);
	}

	public static String getExtension(String fileName) {
		char ch;
		int len;
		if (fileName == null || (len = fileName.length()) == 0 || (ch = fileName.charAt(len - 1)) == '/' // in the case
																											// of a
																											// directory
				|| ch == '\\' || ch == '.') // in the case of . or ..
			return ""; // empty extension
		int dotInd = fileName.lastIndexOf('.'),
				sepInd = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
		if (dotInd <= sepInd)
			return ""; // empty extension
		else
			return fileName.substring(dotInd + 1).toLowerCase();
	}

}

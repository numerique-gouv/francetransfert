/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.domain.utils;

import fr.gouv.culture.francetransfert.application.error.ErrorEnum;
import fr.gouv.culture.francetransfert.domain.exceptions.UploadException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

public class UploadUtils {
	
	private UploadUtils() {
		// private Constructor
	}

	public static String infinity_Date = "2000-12-31";

	public static LocalDate extractStartDateSenderToken(String senderToken) {
		String startDate = extractPartOfString(1, senderToken);
		return convertStringToLocalDateTime(startDate).toLocalDate();
	}



	public static String extractPartOfString(int part, String string) {
		Pattern pattern = Pattern.compile(":");
		String[] items = pattern.split(string, 2);
		if (2 == items.length) {
			return items[part];
		} else {
			String uuid = UUID.randomUUID().toString();
			throw new UploadException(ErrorEnum.TECHNICAL_ERROR.getValue(), uuid);
		}
	}

	public static LocalDateTime convertStringToLocalDateTime(String date) {
		if (null == date) {
			date = infinity_Date;
		}
		//convert String to LocalDateTime
		return LocalDateTime.parse(date);
	}
}

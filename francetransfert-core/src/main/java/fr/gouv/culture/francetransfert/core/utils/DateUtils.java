/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {
	
	private DateUtils() {
		// private Constructor
	}

    private static String infinity_Date = "2000-01-31T22:28:11.882326200";

    public static LocalDateTime convertStringToLocalDateTime(String localDateTime) {
        if (null == localDateTime) {
            localDateTime = infinity_Date;
        }
        //convert String to LocalDateTime
        return LocalDateTime.parse(localDateTime);
    }

    public static LocalDate convertStringToLocalDate(String localDate) {
        if (null == localDate) {
            localDate = infinity_Date;
        }
        //convert String to LocalDate
        return LocalDateTime.parse(localDate).toLocalDate();
    }

    public static String convertLocalDateTimeToString(LocalDateTime localDateTime, String pattern) {
        String result = "";
        if (localDateTime != null) {
            result = localDateTime.format(DateTimeFormatter.ofPattern(pattern));
        }
        return result;
    }

    public static String formatLocalDateTime(String localDateTime) {
        LocalDateTime date = convertStringToLocalDateTime(localDateTime);
        return convertLocalDateTimeToString(date, "dd/MM/yyyy");

    }
}

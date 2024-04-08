/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.utils;

import java.text.DecimalFormat;

import org.apache.commons.io.FileUtils;

public class FTFileUtils {

	private static final DecimalFormat df = new DecimalFormat("#.##");

	public static String byteCountToDisplaySize(double size) {
		String displaySize;

		if (size / FileUtils.ONE_GB >= 1) {
			displaySize = df.format((size / FileUtils.ONE_GB)) + " GB";
		} else if (size / FileUtils.ONE_MB >= 1) {
			displaySize = df.format((size / FileUtils.ONE_MB)) + " MB";
		} else if (size / FileUtils.ONE_KB >= 1) {
			displaySize = df.format((size / FileUtils.ONE_KB)) + " KB";
		} else {
			displaySize = df.format((size)) + " B";
		}
		return displaySize;
	}

}

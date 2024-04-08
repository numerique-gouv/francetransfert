/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TypeStat {

	UPLOAD("upload"), DOWNLOAD("download"), UPLOAD_SATISFACTION("upload_satisfaction"),
	DOWNLOAD_SATISFACTION("download_satisfaction");

	private String value;

}

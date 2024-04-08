/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.model;

import java.util.List;

import lombok.Data;

@Data
public class SnapResponse {

	private List<String> mails;

}

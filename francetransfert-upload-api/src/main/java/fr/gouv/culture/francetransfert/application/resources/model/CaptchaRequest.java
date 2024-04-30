/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import fr.gouv.culture.francetransfert.core.enums.CaptchaTypeEnum;
import lombok.Data;

@Data
public class CaptchaRequest {

	private String challengeId;
	private String userResponse;
	private CaptchaTypeEnum captchaType;

}

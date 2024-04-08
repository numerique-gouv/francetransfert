/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.model;

import fr.gouv.culture.francetransfert.core.enums.CaptchaTypeEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FormulaireContactData {

	private String nom;
	private String prenom;
	@NotBlank
	private String from;
	private String administration;
	@NotBlank
	private String message;
	private String subject;
	// Captcha Info
	private String challengeId;
	private String userResponse;
	private CaptchaTypeEnum captchaType;
}

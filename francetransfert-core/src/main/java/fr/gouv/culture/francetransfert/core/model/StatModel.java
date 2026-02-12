/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.model;

import java.time.LocalDate;

import fr.gouv.culture.francetransfert.core.enums.TypeStat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.Email;

@Data
public class StatModel {

	@NotBlank
	protected String plis;

	protected String hashMail;

	protected String domain;

	protected String date;

	protected TypeStat type;

	@Email
	protected String mailAdress;

}

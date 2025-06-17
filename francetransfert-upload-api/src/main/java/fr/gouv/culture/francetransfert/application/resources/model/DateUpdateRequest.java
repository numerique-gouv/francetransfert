/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import fr.gouv.culture.francetransfert.validator.DateUpdateConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
//@Builder
@NoArgsConstructor
@DateUpdateConstraint(enclosureId = "enclosureId", newDate = "newDate")
public class DateUpdateRequest {

	private String token;

	private String enclosureId;

	private String senderMail;

	@JsonFormat(pattern = "dd-MM-yyyy")
	private LocalDate newDate;
}

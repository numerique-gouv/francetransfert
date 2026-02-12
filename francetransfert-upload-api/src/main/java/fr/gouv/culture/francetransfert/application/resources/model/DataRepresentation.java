/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import fr.gouv.culture.francetransfert.core.utils.SanitizerUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
// @Builder
@NoArgsConstructor
public class DataRepresentation {
	@NotBlank
	@JsonAlias("nomFichier")
	protected String name;

	public String getName() {
		return SanitizerUtil.sanitize(name);
	}

	public void setName(String name) {
		this.name = name;
	}

}

/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import org.owasp.encoder.Encode;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
//@Builder
@NoArgsConstructor
public class DataRepresentation {
	@NotBlank
	@JsonAlias("nomFichier")
	protected String name;

	public String getName() {
		return Encode.forHtml(name);
	}

	public void setName(String name) {
		this.name = name;
	}

}

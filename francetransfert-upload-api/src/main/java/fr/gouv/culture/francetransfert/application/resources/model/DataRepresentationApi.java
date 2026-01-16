/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import fr.gouv.culture.francetransfert.core.utils.SanitizerUtil;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
//@Builder
@NoArgsConstructor
public class DataRepresentationApi {

	@JsonProperty("nomFichier")
	protected String name;

	public String getName() {
		return SanitizerUtil.sanitize(name);
	}

	public void setName(String name) {
		this.name = name;
	}

}

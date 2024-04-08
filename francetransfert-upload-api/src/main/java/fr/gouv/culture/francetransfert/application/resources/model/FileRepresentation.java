/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
//@Builder
@NoArgsConstructor
public class FileRepresentation extends DataRepresentation {

	@NotBlank
	@JsonAlias("idFichier")
	private String fid;
	@JsonAlias("tailleFichier")
	private Long size;

	public FileRepresentation(FileRepresentationApi file) {
		fid = file.getFid();
		size = file.getSize();
		name = file.getName();
	}
}

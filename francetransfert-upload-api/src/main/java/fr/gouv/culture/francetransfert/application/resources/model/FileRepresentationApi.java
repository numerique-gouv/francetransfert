/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileRepresentationApi extends DataRepresentationApi {

	public FileRepresentationApi(FileRepresentation file) {
		fid = file.getFid();
		size = file.getSize();
		name = file.getName();
	}
	

	@JsonProperty("idFichier")
	private String fid;
	@JsonProperty("tailleFichier")
	private long size;
}

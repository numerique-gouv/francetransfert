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
@NoArgsConstructor
public class DownloadPasswordMetaData {
	@NotBlank(message = "EnclosureId obligatoire")
	@JsonAlias("idPli")
	private String enclosure;
	@JsonAlias("courrielUtilisateur")
	private String recipient;
	@JsonAlias("motDePasse")
	private String password;
	private String token;
	private String senderToken;

}

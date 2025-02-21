/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import java.util.List;

import jakarta.validation.constraints.Size;
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
public class SenderInfoRepresentationApi {

	private String idPli;
	private StatusRepresentation statutPli;
	private String typePli;
	private String courrielExpediteur;
	private List<String> destinataires;
	@Size(max = 255)
	private String objet;
	private String dateValidite;
	private Long taillePli;
	private String dateDepot;

}

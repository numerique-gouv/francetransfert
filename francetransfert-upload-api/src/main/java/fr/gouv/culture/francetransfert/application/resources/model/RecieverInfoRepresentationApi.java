/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

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
public class RecieverInfoRepresentationApi {

	private String idPli;
	private StatusRepresentation statutPli;
	private String courrielExpediteur;
	@Size(max = 255)
	private String objet;
	private String dateValidite;
	private Long taillePli;
	private String dateDepot;

}

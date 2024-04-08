/* 
 * Copyright (c) Ministère de la Culture (2022) 
 * 
 * SPDX-License-Identifier: Apache-2.0 
 * License-Filename: LICENSE.txt 
 */ 
 
/* 
 * Copyright (c) Ministère de la Culture (2022) 
 * 
 * SPDX-License-Identifier: Apache-2.0 
 * License-Filename: LICENSE.txt 
 */ 
 
/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
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
public class ValidateUpload {
   
	   @JsonProperty("idPli")
	   private String enclosureId;
	   @JsonProperty("courrielExpediteur")
	   private String senderEmail;
	   @Valid
	   @JsonProperty("fichiers")
	   private FileRepresentation rootFiles;
	   private MultipartFile  fichier;  
	   
	   @JsonProperty("numMorceauFichier")
	   private Integer flowChunkNumber;
	   @JsonProperty("tailleMorceauFichier")
	   private Long flowChunkSize;
	   @JsonProperty("totalMorceauxFichier")
	   private Integer flowTotalChunks;
      
}



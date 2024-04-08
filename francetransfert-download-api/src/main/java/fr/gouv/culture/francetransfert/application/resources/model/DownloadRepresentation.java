/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DownloadRepresentation {

	private LocalDate validUntilDate;
	private String senderEmail;
	private String recipientMail;
	private String message;
	private List<FileRepresentation> rootFiles;
	private List<DirectoryRepresentation> rootDirs;
	private boolean withPassword;
	private boolean pliExiste;
	private boolean checkOSMOSE;
	private boolean checkRESANA;
	private String urlOsmose;
	private String urlResana;
}

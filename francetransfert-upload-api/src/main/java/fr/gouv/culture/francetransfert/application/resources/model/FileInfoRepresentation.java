/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import java.time.LocalDate;
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
public class FileInfoRepresentation {
	private LocalDate validUntilDate;
	private String senderEmail;
	private List<RecipientInfo> recipientsMails;
	private List<RecipientInfo> deletedRecipients;
	@Size(max = 2500)
	private String message;
	private String timestamp;
	private List<FileRepresentation> rootFiles;
	private List<DirectoryRepresentation> rootDirs;
	private boolean withPassword;
	private int downloadCount;
	@Size(max = 255)
	private String subject;
	private String enclosureId;
	private boolean deleted;
	private boolean publicLink;
	private String totalSize;
	private long totalSizeLong;

	private boolean archive;
	private boolean expired;
	private LocalDate archiveUntilDate;

}

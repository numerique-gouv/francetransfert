/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import java.time.LocalDate;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.owasp.encoder.Encode;

import fr.gouv.culture.francetransfert.core.utils.SanitizerUtil;
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
	@Size(max = 101)
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

	public String getMessage() {
		return SanitizerUtil.sanitize(message);
	}

	public String getSubject() {
		return SanitizerUtil.sanitize(subject);
	}

}

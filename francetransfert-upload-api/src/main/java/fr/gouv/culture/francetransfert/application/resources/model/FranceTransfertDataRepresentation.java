/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import fr.gouv.culture.francetransfert.core.utils.SanitizerUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FranceTransfertDataRepresentation {
	private String confirmedSenderId;
	private String senderEmail;
	@Size(min = 1, max = 101)
	private List<String> recipientEmails;
	private String password;
	private Boolean passwordGenerated;
	@Size(max = 2500)
	private String message;
	@Size(max = 255)
	private String subject;
	private Boolean publicLink;
	private int passwordTryCount;
	private int expireDelay;
	private String senderId;
	private String senderToken;
	@Valid
	private List<FileRepresentation> rootFiles;
	@Valid
	private List<DirectoryRepresentation> rootDirs;

	private Locale language;
	private Boolean zipPassword;

	private String source;
	private boolean envoiMdpDestinataires;

	private String uploadToken;

	public String getConfirmedSenderId() {
		return confirmedSenderId;
	}

	public void setConfirmedSenderId(String confirmedSenderId) {
		this.confirmedSenderId = confirmedSenderId;
	}

	public String getSenderEmail() {
		return senderEmail;
	}

	public void setSenderEmail(String senderEmail) {
		this.senderEmail = senderEmail;
	}

	public List<String> getRecipientEmails() {
		return recipientEmails;
	}

	public void setRecipientEmails(List<String> recipientEmails) {
		this.recipientEmails = recipientEmails;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Boolean getPasswordGenerated() {
		return passwordGenerated;
	}

	public void setPasswordGenerated(Boolean passwordGenerated) {
		this.passwordGenerated = passwordGenerated;
	}

	public String getMessage() {
		if (StringUtils.isNotBlank(message)) {
			return SanitizerUtil.sanitize(message);
		} else {
			return "";
		}
	}

	public String getMessageNotEncoded() {
		if (StringUtils.isNotBlank(message)) {
			return message;
		} else {
			return "";
		}
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getLengthMessage() {
		if (StringUtils.isNotBlank(message)) {
			return message.length();
		} else {
			return 0;
		}
	}

	public String getSubject() {
		if (StringUtils.isNotBlank(subject)) {
			return SanitizerUtil.sanitize(subject);
		} else {
			return "";
		}
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public Boolean getPublicLink() {
		return publicLink;
	}

	public void setPublicLink(Boolean publicLink) {
		this.publicLink = publicLink;
	}

	public int getPasswordTryCount() {
		return passwordTryCount;
	}

	public void setPasswordTryCount(int passwordTryCount) {
		this.passwordTryCount = passwordTryCount;
	}

	public int getExpireDelay() {
		return expireDelay;
	}

	public void setExpireDelay(int expireDelay) {
		this.expireDelay = expireDelay;
	}

	public String getSenderId() {
		return senderId;
	}

	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}

	public String getSenderToken() {
		return senderToken;
	}

	public void setSenderToken(String senderToken) {
		this.senderToken = senderToken;
	}

	public List<FileRepresentation> getRootFiles() {
		return rootFiles;
	}

	public void setRootFiles(List<FileRepresentation> rootFiles) {
		this.rootFiles = rootFiles;
	}

	public List<DirectoryRepresentation> getRootDirs() {
		return rootDirs;
	}

	public void setRootDirs(List<DirectoryRepresentation> rootDirs) {
		this.rootDirs = rootDirs;
	}

	public Locale getLanguage() {
		return language;
	}

	public void setLanguage(Locale language) {
		this.language = language;
	}

	public Boolean getZipPassword() {
		return zipPassword;
	}

	public void setZipPassword(Boolean zipPassword) {
		this.zipPassword = zipPassword;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public boolean isEnvoiMdpDestinataires() {
		return envoiMdpDestinataires;
	}

	public void setEnvoiMdpDestinataires(boolean envoiMdpDestinataires) {
		this.envoiMdpDestinataires = envoiMdpDestinataires;
	}

	public String getUploadToken() {
		return uploadToken;
	}

	public void setUploadToken(String uploadToken) {
		this.uploadToken = uploadToken;
	}

}

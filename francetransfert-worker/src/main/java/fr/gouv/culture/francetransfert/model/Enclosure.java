/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;

import fr.gouv.culture.francetransfert.core.enums.EnclosureKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.utils.DateUtils;
import fr.gouv.culture.francetransfert.core.utils.RedisUtils;
import fr.gouv.culture.francetransfert.utils.WorkerUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Enclosure {

	private String guid;

	private List<RootData> rootFiles;

	private List<RootData> rootDirs;

	private int countElements;

	private String totalSize;

	private String expireDate;

	private String expireArchiveDate;

	private String sender;

	private List<Recipient> recipients;

	private List<Recipient> notDownloadRecipients;

	private String message;

	private String subject;

	private boolean withPassword;

	private String urlDownload;

	private List<String> recipientDownloadInProgress;

	private String password;

	private String urlAdmin;

	private boolean publicLink;

	private List<ScanInfo> virusScan;

	private String errorCode;

	private boolean ckeckRESANA;

	private boolean ckeckOSMOSE;

	private String urlResana;

	private String urlOsmose;

	private String fileError;
	// ---
	private String statut;

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getSender() {
		return sender;
	}

	/*
	 * public String getSender() { return }
	 */
	public static Enclosure build(String enclosureId, RedisManager redisManager) throws MetaloadException {

		Locale language = Locale.FRANCE;

		String redisLang = RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.LANGUAGE.getKey());
		if (StringUtils.isNotBlank(redisLang)) {
			try {
				language = LocaleUtils.toLocale(
						RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.LANGUAGE.getKey()));
			} catch (Exception eL) {
				// LOGGER.error("Error while getting local", eL);
			}
		}

		List<RootData> filesOfEnclosure = new ArrayList<>();
		for (Map.Entry<String, Long> rootFile : RedisUtils.getRootFilesWithSize(redisManager, enclosureId).entrySet()) {
			filesOfEnclosure.add(
					RootData.builder().name(rootFile.getKey()).extension(WorkerUtils.getExtension(rootFile.getKey()))
							.size(WorkerUtils.getFormatFileSizeLanguage(language, rootFile.getValue()))
							.nameWithoutExtension(FilenameUtils.removeExtension(rootFile.getKey())).build());
		}
		List<RootData> dirsOfEnclosure = new ArrayList<>();
		for (Map.Entry<String, Long> rootDir : RedisUtils.getRootDirsWithSize(redisManager, enclosureId).entrySet()) {
			dirsOfEnclosure.add(RootData.builder().name(rootDir.getKey())
					.size(WorkerUtils.getFormatFileSizeLanguage(language, rootDir.getValue()))
					.nameWithoutExtension(rootDir.getKey()).build());
		}

		String totalSize = WorkerUtils.getFormatFileSizeLanguage(language,
				RedisUtils.getTotalSizeEnclosure(redisManager, enclosureId));

		String senderEnclosure = RedisUtils.getEmailSenderEnclosure(redisManager, enclosureId);
		List<Recipient> recipientsEnclosure = new ArrayList<>();
		for (Map.Entry<String, String> recipient : RedisUtils.getRecipientsEnclosure(redisManager, enclosureId)
				.entrySet()) {
			recipientsEnclosure.add(Recipient.builder().mail(recipient.getKey()).id(recipient.getValue())
					.suppressionLogique(RedisUtils.isRecipientDeleted(redisManager, recipient.getValue())).build());
		}
		Map<String, String> enclosureRedis = RedisUtils.getEnclosure(redisManager, enclosureId);
		String expireEnclosureDate = DateUtils
				.formatLocalDateTime(enclosureRedis.get(EnclosureKeysEnum.EXPIRED_TIMESTAMP.getKey()));
		String expireEnclosureArchiveDate = DateUtils
				.formatLocalDateTime(enclosureRedis.get(EnclosureKeysEnum.EXPIRED_TIMESTAMP_ARCHIVE.getKey()));
		String message = enclosureRedis.get(EnclosureKeysEnum.MESSAGE.getKey());
		if (StringUtils.isBlank(message)) {
			message = "";
		}
		String subject = enclosureRedis.get(EnclosureKeysEnum.SUBJECT.getKey());
		if (StringUtils.isBlank(subject)) {
			subject = "";
		}
		String password = enclosureRedis.get(EnclosureKeysEnum.PASSWORD.getKey());
		boolean withPassword = password != null && !password.isEmpty();
		password = "";

		Map<String, String> scanJsonList = redisManager
				.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE_VIRUS.getKey(enclosureId));
		List<ScanInfo> scanList = scanJsonList.values().stream().map(json -> {
			return new Gson().fromJson(json, ScanInfo.class);
		}).collect(Collectors.toList());

		// ---
		String statut = enclosureRedis.get(EnclosureKeysEnum.STATUS_CODE.getKey());

		return Enclosure.builder().guid(enclosureId).rootFiles(filesOfEnclosure).rootDirs(dirsOfEnclosure)
				.countElements(filesOfEnclosure.size() + dirsOfEnclosure.size()).totalSize(totalSize)
				.expireDate(expireEnclosureDate).sender(senderEnclosure).recipients(recipientsEnclosure)
				.message(message).subject(subject).withPassword(withPassword).statut(statut).virusScan(scanList)
				.build();
	}
}

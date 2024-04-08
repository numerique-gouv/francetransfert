/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.services;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
//import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Service;

import fr.gouv.culture.francetransfert.application.error.ErrorEnum;
import fr.gouv.culture.francetransfert.application.error.MaxTryException;
import fr.gouv.culture.francetransfert.application.error.PasswordException;
import fr.gouv.culture.francetransfert.application.error.UnauthorizedAccessException;
import fr.gouv.culture.francetransfert.application.error.UnauthorizedApiAccessException;
import fr.gouv.culture.francetransfert.application.resources.model.DirectoryRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.Download;
import fr.gouv.culture.francetransfert.application.resources.model.DownloadPasswordMetaData;
import fr.gouv.culture.francetransfert.application.resources.model.DownloadRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.FileRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.RecipientInfo;
import fr.gouv.culture.francetransfert.core.enums.EnclosureKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RecipientKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisQueueEnum;
import fr.gouv.culture.francetransfert.core.enums.RootDirKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RootFileKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.StatutEnum;
import fr.gouv.culture.francetransfert.core.enums.TypeStat;
import fr.gouv.culture.francetransfert.core.enums.ValidationErrorEnum;
import fr.gouv.culture.francetransfert.core.error.ApiValidationError;
import fr.gouv.culture.francetransfert.core.exception.ApiValidationException;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.exception.StatException;
import fr.gouv.culture.francetransfert.core.exception.StorageException;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.services.StorageManager;
import fr.gouv.culture.francetransfert.core.utils.Base64CryptoService;
import fr.gouv.culture.francetransfert.core.utils.DateUtils;
import fr.gouv.culture.francetransfert.core.utils.RedisUtils;
import fr.gouv.culture.francetransfert.core.utils.StringUploadUtils;
import fr.gouv.culture.francetransfert.domain.exceptions.DownloadException;
import fr.gouv.culture.francetransfert.domain.exceptions.ExpirationEnclosureException;
import fr.gouv.culture.francetransfert.domain.exceptions.InvalidHashException;

@Service
public class DownloadServices {

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadServices.class);

	@Value("${enclosure.max.download}")
	private int maxDownload;

	@Value("${enclosure.max.password.try}")
	private int maxPasswordTry;

	@Value("${bucket.prefix}")
	private String bucketPrefix;

	@Value("${bucket.expire.link:2}")
	private int expireLinkTime;

	@Value("#{${api.key}}")
	Map<String, Map<String, String[]>> apiKey;

	@Autowired
	private StorageManager storageManager;

	@Autowired
	private RedisManager redisManager;

	@Autowired
	private Base64CryptoService base64CryptoService;

	@Autowired
	private StringUploadUtils stringUploadUtils;

	@Value("${resana.mail.url:test}")
	private String urlPatternResana;

	@Value("${osmose.mail.url:test}")
	private String urlPatternOsmose;

	public Download getTelechargementPli(DownloadPasswordMetaData downloadMeta, String headerAddr, String remoteAddr)
			throws ApiValidationException, MetaloadException, StatException {

		if (StringUtils.isBlank(headerAddr)) {
			throw new UnauthorizedAccessException("Erreur d’authentification : aucun objet de réponse renvoyé");
		}

		String recipientMail = downloadMeta.getRecipient();
		String enclosureId = downloadMeta.getEnclosure();
		String password = downloadMeta.getPassword();
		String recipientIdRedis;

		List<ApiValidationError> errorList = new ArrayList<ApiValidationError>();

		validDomainHeader(headerAddr, recipientMail);
		validIpAddress(headerAddr, remoteAddr);

		List<RecipientInfo> recipientsMails = new ArrayList<>();

		ApiValidationError packageIdChecked = validPackageId(enclosureId);
		errorList.add(packageIdChecked);

		if (packageIdChecked == null) {
			recipientIdRedis = RedisUtils.getRecipientId(redisManager, downloadMeta.getEnclosure(),
					downloadMeta.getRecipient().toLowerCase());

			ApiValidationError userMailChecked = validUserMail(recipientMail);
			ApiValidationError statusChecked = validStatus(enclosureId);
			ApiValidationError recipientChecked = validRecipient(enclosureId, recipientsMails, recipientMail);
			ApiValidationError clientHeaderChecked = validClientHeader(headerAddr, remoteAddr, password, enclosureId,
					recipientIdRedis);

			errorList.add(userMailChecked);
			errorList.add(statusChecked);
			errorList.add(recipientChecked);
			errorList.add(clientHeaderChecked);
		}

		errorList.removeIf(Objects::isNull);

		if (CollectionUtils.isEmpty(errorList)) {
			Download validPackage = new Download();

			String recipientId = RedisUtils.getRecipientId(redisManager, enclosureId, recipientMail);

			downloadProgress(enclosureId, recipientId);
			validPackage = getDownloadUrl(enclosureId);
			return validPackage;

		} else {
			LOGGER.info("Error while generating downloadurl from public api {}", errorList);
			throw new ApiValidationException(errorList);
		}

	}

	private void validDomainHeader(String headerAddr, String sender) {

		// get domain from properties
		String[] domaine = apiKey.getOrDefault(headerAddr, new HashMap<String, String[]>()).getOrDefault("domaine",
				new String[0]);
		String senderDomaine = stringUploadUtils.extractDomainNameFromEmailAddress(sender);
		if (!domaine[0].equals("*")) {
			if (domaine.length == 0 || StringUtils.isBlank(senderDomaine)
					|| !StringUtils.containsIgnoreCase(domaine[0], senderDomaine)) {
				throw new UnauthorizedApiAccessException("Erreur d’authentification : aucun objet de réponse renvoyé");
			}
		}

	}

	private void validIpAddress(String headerAddr, String remoteAddr) {

		String[] ips = apiKey.getOrDefault(headerAddr, new HashMap<String, String[]>()).getOrDefault("ips",
				new String[0]);
		boolean ipMatch = Arrays.stream(ips).anyMatch(ip -> {
			IpAddressMatcher ipAddressMatcher = new IpAddressMatcher(ip);
			return ipAddressMatcher.matches(remoteAddr);
		});

		if (!ipMatch) {
			throw new UnauthorizedApiAccessException("Erreur d’authentification : aucun objet de réponse renvoyé");
		}

	}

	private ApiValidationError validPackageId(String enclosureId) throws MetaloadException {
		ApiValidationError validIdPli = null;

		String senderEnclosureMail = RedisUtils.getEmailSenderEnclosure(redisManager, enclosureId);
		if (StringUtils.isEmpty(senderEnclosureMail)) {
			validIdPli = new ApiValidationError();
			validIdPli.setCodeChamp(ValidationErrorEnum.FT205.getCodeChamp());
			validIdPli.setNumErreur(ValidationErrorEnum.FT205.getNumErreur());
			validIdPli.setLibelleErreur(ValidationErrorEnum.FT205.getLibelleErreur());

		}
		return validIdPli;
	}

	private ApiValidationError validStatus(String enclosureId) throws MetaloadException {
		ApiValidationError validStatus = null;

		Map<String, String> enclosureRedis = RedisUtils.getEnclosure(redisManager, enclosureId);
		String statusCode = enclosureRedis.get(EnclosureKeysEnum.STATUS_CODE.getKey());
		if (!StatutEnum.PAT.getCode().equals(statusCode) && !StatutEnum.ECT.getCode().equals(statusCode)) {
			validStatus = new ApiValidationError();
			validStatus.setCodeChamp(ValidationErrorEnum.FT601.getCodeChamp());
			validStatus.setNumErreur(ValidationErrorEnum.FT601.getNumErreur());
			validStatus.setLibelleErreur(ValidationErrorEnum.FT601.getLibelleErreur());
		}
		return validStatus;
	}

	private ApiValidationError validUserMail(String recipientMail) {
		ApiValidationError validUserMail = null;

		if (StringUtils.isNotEmpty(recipientMail)) {

			if (!stringUploadUtils.isValidEmail(recipientMail)) {

				validUserMail = new ApiValidationError();
				validUserMail.setCodeChamp(ValidationErrorEnum.FT06.getCodeChamp());
				validUserMail.setNumErreur(ValidationErrorEnum.FT06.getNumErreur());
				validUserMail.setLibelleErreur(ValidationErrorEnum.FT06.getLibelleErreur());

			} else {

				if (!stringUploadUtils.isValidEmailIgni(recipientMail)) {
					validUserMail = new ApiValidationError();
					validUserMail.setCodeChamp(ValidationErrorEnum.FT04.getCodeChamp());
					validUserMail.setNumErreur(ValidationErrorEnum.FT04.getNumErreur());
					validUserMail.setLibelleErreur(ValidationErrorEnum.FT04.getLibelleErreur());
				}

			}

		} else {
			validUserMail = new ApiValidationError();
			validUserMail.setCodeChamp(ValidationErrorEnum.FT05.getCodeChamp());
			validUserMail.setNumErreur(ValidationErrorEnum.FT05.getNumErreur());
			validUserMail.setLibelleErreur(ValidationErrorEnum.FT05.getLibelleErreur());
		}

		return validUserMail;
	}

	public ApiValidationError validClientHeader(String headerAddr, String remoteAddr, String password,
			String enclosureId, String recipientId) throws MetaloadException, StatException {

		ApiValidationError validPassword = null;
		int passwordCountTry = 0;

		String[] passwordProp = apiKey.getOrDefault(headerAddr, new HashMap<String, String[]>())
				.getOrDefault("password", new String[2]);

		if (StringUtils.isEmpty(password)) {
			if (!Boolean.valueOf(passwordProp[0])) {
				validPassword = new ApiValidationError();
				validPassword.setCodeChamp(ValidationErrorEnum.FT604.getCodeChamp());
				validPassword.setNumErreur(ValidationErrorEnum.FT604.getNumErreur());
				validPassword.setLibelleErreur(ValidationErrorEnum.FT604.getLibelleErreur());
			}
		} else {
			String passwordUnHashed = "";
			passwordCountTry = RedisUtils.getPasswordTryCountPerRecipient(redisManager, recipientId);
			passwordUnHashed = getUnhashedPassword(enclosureId);
			if (password == null || passwordUnHashed == null || !password.equals(passwordUnHashed)) {
				passwordUnHashed = "";
				RedisUtils.incrementNumberOfPasswordTry(redisManager, recipientId);
				redisManager.hsetString(RedisKeysEnum.FT_RECIPIENT.getKey(recipientId),
						RecipientKeysEnum.LAST_PASSWORD_TRY.getKey(), LocalDateTime.now().toString(), -1);
				if ((passwordCountTry + 1) >= maxPasswordTry) {
					validPassword = new ApiValidationError();
					validPassword.setCodeChamp(ValidationErrorEnum.FT606.getCodeChamp());
					validPassword.setNumErreur(ValidationErrorEnum.FT606.getNumErreur());
					validPassword.setLibelleErreur(ValidationErrorEnum.FT606.getLibelleErreur());
				} else {
					validPassword = new ApiValidationError();
					validPassword.setCodeChamp(ValidationErrorEnum.FT605.getCodeChamp());
					validPassword.setNumErreur(ValidationErrorEnum.FT605.getNumErreur());
					validPassword.setLibelleErreur(ValidationErrorEnum.FT605.getLibelleErreur());
				}
			}
		}

		return validPassword;

	}

	private ApiValidationError validRecipient(String enclosureId, List<RecipientInfo> recipientsMails, String recipient)
			throws MetaloadException {

		ApiValidationError validRecipient = null;
		boolean checkRecipientExist = false;

		for (Map.Entry<String, String> recipients : RedisUtils.getRecipientsEnclosure(redisManager, enclosureId)
				.entrySet()) {
			RecipientInfo recinfo = buildRecipient(recipients.getKey(), enclosureId);
			if (recinfo.getRecipientMail().equals(recipient.toLowerCase())) {
				if (!recinfo.isDeleted()) {
					checkRecipientExist = true;
					if (recinfo.getNumberOfDownload() >= 5) {
						validRecipient = new ApiValidationError();
						validRecipient.setCodeChamp(ValidationErrorEnum.FT603.getCodeChamp());
						validRecipient.setNumErreur(ValidationErrorEnum.FT603.getNumErreur());
						validRecipient.setLibelleErreur(ValidationErrorEnum.FT603.getLibelleErreur());
					}
				}

			}

		}

		if (!checkRecipientExist) {
			validRecipient = new ApiValidationError();
			validRecipient.setCodeChamp(ValidationErrorEnum.FT602.getCodeChamp());
			validRecipient.setNumErreur(ValidationErrorEnum.FT602.getNumErreur());
			validRecipient.setLibelleErreur(ValidationErrorEnum.FT602.getLibelleErreur());
		}

		return validRecipient;
	}

	private RecipientInfo buildRecipient(String email, String enclosureId) throws MetaloadException {
		String recipientId = RedisUtils.getRecipientId(redisManager, enclosureId, email);
		Set<String> downloadDate = redisManager.smembersString(RedisKeysEnum.FT_Download_Date.getKey(recipientId));
		ArrayList<String> downloadDates = new ArrayList<String>(downloadDate);
		int nbDownload = RedisUtils.getNumberOfDownloadsPerRecipient(redisManager, recipientId);
		boolean deleted = RedisUtils.isRecipientDeleted(redisManager, recipientId);
		RecipientInfo recipient = new RecipientInfo(email, nbDownload, deleted);

		return recipient;
	}

	public Download generateDownloadUrl(DownloadPasswordMetaData downloadMeta)
			throws ExpirationEnclosureException, UnsupportedEncodingException, MetaloadException, StorageException {

		String recipientIdRedis;
		String recipientMail = downloadMeta.getRecipient();
		boolean validSenderToken = false;

		if (downloadMeta.getSenderToken() != null) {
			redisManager.validateToken(downloadMeta.getRecipient().toLowerCase(), downloadMeta.getSenderToken());
			recipientIdRedis = RedisUtils.getRecipientId(redisManager, downloadMeta.getEnclosure(),
					downloadMeta.getRecipient().toLowerCase());
			validSenderToken = true;
		} else {
			recipientIdRedis = downloadMeta.getToken();
		}

		if (!stringUploadUtils.isValidEmail(recipientMail)) {
			recipientMail = base64CryptoService.base64Decoder(recipientMail);
		}

		recipientMail = recipientMail.toLowerCase();

		checkDeletePlis(downloadMeta.getEnclosure());
		validateDownloadAuthorization(downloadMeta.getEnclosure(), recipientMail, recipientIdRedis);
		if (!validSenderToken) {
			validatePassword(downloadMeta.getEnclosure(), downloadMeta.getPassword(), recipientIdRedis);
		}
		downloadProgress(downloadMeta.getEnclosure(), recipientIdRedis);
		String statMessage = TypeStat.DOWNLOAD + ";" + downloadMeta.getEnclosure() + ";" + recipientMail;
		redisManager.publishFT(RedisQueueEnum.STAT_QUEUE.getValue(), statMessage);
		return getDownloadUrl(downloadMeta.getEnclosure());

	}

	public Download generateDownloadUrlWithPassword(DownloadPasswordMetaData downloadMeta)
			throws ExpirationEnclosureException, UnsupportedEncodingException, MetaloadException, StorageException {

		String recipientIdRedis;
		String recipientMail = downloadMeta.getRecipient();

		if (downloadMeta.getSenderToken() != null) {
			redisManager.validateToken(downloadMeta.getRecipient().toLowerCase(), downloadMeta.getSenderToken());
			recipientIdRedis = RedisUtils.getRecipientId(redisManager, downloadMeta.getEnclosure(),
					downloadMeta.getRecipient().toLowerCase());

		} else {
			recipientIdRedis = downloadMeta.getToken();
		}

		if (!stringUploadUtils.isValidEmail(recipientMail)) {
			recipientMail = base64CryptoService.base64Decoder(recipientMail);
		}

		recipientMail = recipientMail.toLowerCase();

		checkDeletePlis(downloadMeta.getEnclosure());
		validateDownloadAuthorization(downloadMeta.getEnclosure(), recipientMail, recipientIdRedis);
		validatePassword(downloadMeta.getEnclosure(), downloadMeta.getPassword(), recipientIdRedis);
		downloadProgress(downloadMeta.getEnclosure(), recipientIdRedis);
		String statMessage = TypeStat.DOWNLOAD + ";" + downloadMeta.getEnclosure() + ";" + recipientMail;
		redisManager.publishFT(RedisQueueEnum.STAT_QUEUE.getValue(), statMessage);
		return getDownloadUrl(downloadMeta.getEnclosure());

	}

	public Download generatePublicDownload(String enclosureId, String password)
			throws MetaloadException, UnsupportedEncodingException {
		validatePassword(enclosureId, password, null);
		RedisUtils.incrementNumberOfDownloadPublic(redisManager, enclosureId);
		String statMessage = TypeStat.DOWNLOAD + ";" + enclosureId;
		redisManager.publishFT(RedisQueueEnum.STAT_QUEUE.getValue(), statMessage);
		return getDownloadUrl(enclosureId);
	}

	public String getNumberOfDownloadPublic(String enclosureId) {
		Map<String, String> enclosureMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
		if (enclosureMap != null) {
			return enclosureMap.get(EnclosureKeysEnum.PUBLIC_DOWNLOAD_COUNT.getKey());
		} else {
			throw new DownloadException(ErrorEnum.WRONG_ENCLOSURE.getValue(), enclosureId);
		}
	}

	public DownloadRepresentation getDownloadInfoConnect(String enclosureId, String recipient)
			throws UnsupportedEncodingException, ExpirationEnclosureException, MetaloadException, StorageException {

		String recipientIdRedis = RedisUtils.getRecipientId(redisManager, enclosureId, recipient);
		DownloadRepresentation downloadRepresentation = getDownloadInfo(enclosureId, recipientIdRedis, recipient);

		return downloadRepresentation;

	}

	public DownloadRepresentation getDownloadInfo(String enclosureId, String senderToken, String recipientMailInBase64)
			throws UnsupportedEncodingException, ExpirationEnclosureException, MetaloadException, StorageException {

		// validate Enclosure download right
		String recipientMail = recipientMailInBase64;

		if (!stringUploadUtils.isValidEmail(recipientMailInBase64)) {
			recipientMail = base64CryptoService.base64Decoder(recipientMailInBase64);
		}

		checkDeletePlis(enclosureId);
		LocalDate expirationDate = validateDownloadAuthorization(enclosureId, recipientMail, senderToken);

		try {

			String passwordRedis = RedisUtils.getEnclosureValue(redisManager, enclosureId,
					EnclosureKeysEnum.PASSWORD.getKey());
			String message = RedisUtils.getEnclosureValue(redisManager, enclosureId,
					EnclosureKeysEnum.MESSAGE.getKey());
			String senderMail = RedisUtils.getEmailSenderEnclosure(redisManager, enclosureId);

			List<FileRepresentation> rootFiles = getRootFiles(enclosureId);
			List<DirectoryRepresentation> rootDirs = getRootDirs(enclosureId);
			boolean checkRESANA = false;
			boolean checkOSMOSE = false;
			String urlOsmose = "";
			String urlResana = "";

			if (StringUtils.isNotBlank(recipientMail)) {
				checkRESANA = redisManager.sexists(RedisKeysEnum.FT_DOMAINS_MAILS_RESANA.getKey(""),
						recipientMail.toLowerCase());
				if (checkRESANA) {
					urlResana = MessageFormat.format(urlPatternResana, enclosureId, recipientMail);
				}
				checkOSMOSE = redisManager.sexists(RedisKeysEnum.FT_DOMAINS_MAILS_OSMOSE.getKey(""),
						recipientMail.toLowerCase());
				if (checkOSMOSE) {
					urlOsmose = MessageFormat.format(urlPatternOsmose, enclosureId, recipientMail);
				}
			}

			DownloadRepresentation downloadRepresentation = DownloadRepresentation.builder()
					.validUntilDate(expirationDate).senderEmail(senderMail).recipientMail(recipientMail)
					.message(message).rootFiles(rootFiles).rootDirs(rootDirs)
					.withPassword(!StringUtils.isEmpty(passwordRedis)).checkRESANA(checkRESANA).checkOSMOSE(checkOSMOSE)
					.urlOsmose(urlOsmose).urlResana(urlResana).build();

			passwordRedis = null;

			return downloadRepresentation;
		} catch (Exception e) {
			throw new DownloadException("Cannot get Download Info : " + e.getMessage(), enclosureId, e);
		}
	}

	public DownloadRepresentation getDownloadInfoPublic(String enclosureId)
			throws ExpirationEnclosureException, MetaloadException {
		checkDeletePlis(enclosureId);
		LocalDate expirationDate = validateDownloadAuthorizationPublic(enclosureId);
		try {
			List<FileRepresentation> rootFiles = getRootFiles(enclosureId);
			List<DirectoryRepresentation> rootDirs = getRootDirs(enclosureId);
			return DownloadRepresentation.builder().validUntilDate(expirationDate).rootFiles(rootFiles)
					.rootDirs(rootDirs).build();
		} catch (Exception e) {
			throw new DownloadException("Cannot get Download Info : " + e.getMessage(), enclosureId, e);
		}
	}

	private void checkDeletePlis(String enclosureId) {
		Map<String, String> tokenMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ADMIN_TOKEN.getKey(enclosureId));

		if (tokenMap.size() == 0) {
			throw new DownloadException(ErrorEnum.DELETED_ENCLOSURE.getValue(), enclosureId);
		}
	}

	private Download getDownloadUrl(String enclosureId) throws DownloadException {
		try {
			String bucketName = RedisUtils.getBucketName(redisManager, enclosureId, bucketPrefix);

			String fileToDownload = storageManager.getZippedEnclosureName(enclosureId);
			int expireInMinutes = expireLinkTime; // periode to exipre the generated URL
			String downloadURL = storageManager.generateDownloadURL(bucketName, fileToDownload, expireInMinutes)
					.toString();
			Download link = Download.builder().lienTelechargement(downloadURL).build();
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
					EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.ECT.getCode(), -1);
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
					EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.ECT.getWord(), -1);
			return link;
		} catch (Exception e) {
			throw new DownloadException("Cannot get Download URL : " + e.getMessage(), enclosureId, e);
		}
	}

	/**
	 * Method to validate download authorization : validate number of download,
	 * validate expiration date and validate recipientId sended by the front
	 *
	 * @param enclosureId
	 * @param recipientMail
	 * @param recipientId
	 * @return enclosure expiration Date
	 * @throws MetaloadException
	 * @throws ExpirationEnclosureException
	 */
	private LocalDate validateDownloadAuthorization(String enclosureId, String recipientMail, String recipientId)
			throws InvalidHashException, MetaloadException, StorageException {
		Boolean recipientDeleted = false;
		String bucketName = RedisUtils.getBucketName(redisManager, enclosureId, bucketPrefix);

		validateRecipientId(enclosureId, recipientMail, recipientId);

		if (StringUtils.isNotBlank(recipientMail)) {
			recipientDeleted = RedisUtils.isRecipientDeleted(redisManager, recipientId);
		}
		if (!recipientDeleted) {

			String fileToDownload = storageManager.getZippedEnclosureName(enclosureId);
			String hashFileFromS3 = storageManager.getEtag(bucketName, fileToDownload);
			String hashFileFromRedis = RedisUtils.getHashFileFromredis(redisManager, enclosureId);

			if (StringUtils.isNotBlank(hashFileFromRedis) && !hashFileFromRedis.equals(hashFileFromS3)) {
				LOGGER.warn("msgtype: INVALID_HASH || enclosure: {} || recipient: {}", enclosureId, recipientMail);
				throw new InvalidHashException("Hash incorrect pour le pli " + enclosureId);
			}
			validateNumberOfDownload(recipientId, enclosureId);
			LocalDate expirationDate = validateExpirationDate(enclosureId);
			return expirationDate;
		} else {
			throw new ExpirationEnclosureException("Vous ne pouvez plus telecharger les fichiers de l'enclosure : "
					+ enclosureId + " recipient : " + recipientMail);
		}

	}

	private LocalDate validateExpirationDate(String enclosureId)
			throws ExpirationEnclosureException, MetaloadException {
		LocalDate expirationDate = DateUtils.convertStringToLocalDate(
				RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.EXPIRED_TIMESTAMP.getKey()));
		if (LocalDate.now().isAfter(expirationDate)) {
			throw new ExpirationEnclosureException("Vous ne pouvez plus telecharger ces fichiers");
		}
		return expirationDate;
	}

	private int validateNumberOfDownload(String recipientId, String enclosureId) throws MetaloadException {
		int numberOfDownload = RedisUtils.getNumberOfDownloadsPerRecipient(redisManager, recipientId);
		if (maxDownload <= numberOfDownload) {
			LOGGER.error("DOWNLOAD_LIMIT for enclosure {}, for recipient {}", enclosureId, recipientId);
			throw new DownloadException(ErrorEnum.DOWNLOAD_LIMIT.getValue(), enclosureId);
		}
		return numberOfDownload;
	}

	private void validateRecipientId(String enclosureId, String recipientMail, String recipientId) {
		try {
			Map<String, String> recList = RedisUtils.getRecipientsEnclosure(redisManager, enclosureId);
			String recipientIdRedis = recList.get(recipientMail);
			if (!recipientIdRedis.equals(recipientId)) {
				throw new DownloadException("NewRecipient id send not equals to Redis recipient id for this enclosure",
						enclosureId);
			}
		} catch (Exception e) {
			throw new DownloadException("Error while validating recipient Id : " + e.getMessage(), enclosureId, e);
		}
	}

	public String getRecipientId(String enclosureId, String recipientParam)
			throws MetaloadException, UnsupportedEncodingException {

		String recipientMail;
		String recipientId = null;
		if (StringUtils.isNotBlank(recipientParam)) {
			// Get recipientid from enclosure
			if (!stringUploadUtils.isValidEmail(recipientParam)) {
				recipientMail = base64CryptoService.base64Decoder(recipientParam);
			} else {
				recipientMail = recipientParam;
			}
			recipientId = RedisUtils.getRecipientId(redisManager, enclosureId, recipientMail);
		}

		return recipientId;

	}

	public void validatePassword(String enclosureId, String password, String recipientId)
			throws UnsupportedEncodingException, MetaloadException {
		String passwordUnHashed = "";
		int passwordCountTry = 0;
		Boolean recipientDeleted = false;

		Map<String, String> enclosureMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
		Boolean publicLink = Boolean.valueOf(enclosureMap.get(EnclosureKeysEnum.PUBLIC_LINK.getKey()));

		try {
			passwordUnHashed = getUnhashedPassword(enclosureId);
		} catch (Exception e) {
			passwordUnHashed = "";
			throw new DownloadException("Error Unhashing password", enclosureId, e);
		}

		// If public onlycheck password
		if (publicLink) {
			if (!(password != null && passwordUnHashed != null && password.equals(passwordUnHashed))) {
				passwordUnHashed = "";
				throw new PasswordException(ErrorEnum.WRONG_PASSWORD.getValue(), enclosureId, passwordCountTry + 1);
			}
			passwordUnHashed = "";
			return;
		}

		// If not publicLink and no recipient fail
		if (StringUtils.isBlank(recipientId)) {
			throw new DownloadException("Missing recipientId to validate enclosure download : " + enclosureId,
					enclosureId);
		}

		// Check recipientId valid for enclosure
		recipientDeleted = RedisUtils.isRecipientDeleted(redisManager, recipientId);
		boolean recipientInEnclosure = RedisUtils.checkRecipientIdInEnclosure(redisManager, enclosureId, recipientId);
		if (recipientDeleted || !recipientInEnclosure) {
			passwordUnHashed = "";
			throw new ExpirationEnclosureException("Vous ne pouvez plus telecharger les fichiers de l'enclosure : "
					+ enclosureId + " recipient : " + recipientId);
		}

		passwordCountTry = RedisUtils.getPasswordTryCountPerRecipient(redisManager, recipientId);

		if (!(password != null && passwordUnHashed != null && password.equals(passwordUnHashed))) {
			passwordUnHashed = "";
			RedisUtils.incrementNumberOfPasswordTry(redisManager, recipientId);
			redisManager.hsetString(RedisKeysEnum.FT_RECIPIENT.getKey(recipientId),
					RecipientKeysEnum.LAST_PASSWORD_TRY.getKey(), LocalDateTime.now().toString(), -1);
			if ((passwordCountTry + 1) >= maxPasswordTry) {
				throw new MaxTryException("Nombre d'essais maximum atteint", enclosureId);
			}
			throw new PasswordException(ErrorEnum.WRONG_PASSWORD.getValue(), enclosureId, passwordCountTry + 1);
		} else {
			passwordUnHashed = "";
			if (passwordCountTry < maxPasswordTry) {
				RedisUtils.resetPasswordTryCountPerRecipient(redisManager, recipientId);
			} else {
				throw new MaxTryException("Nombre d'essais maximum atteint", enclosureId);
			}
		}
	}

	private String getUnhashedPassword(String enclosureId) throws MetaloadException, StatException {
		String passwordRedis = null;
		passwordRedis = RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.PASSWORD.getKey());
		if (passwordRedis != null && !StringUtils.isEmpty(passwordRedis)) {
			return base64CryptoService.aesDecrypt(passwordRedis);
		}
		return passwordRedis;
	}

	private void downloadProgress(String enclosureId, String recipientId) throws DownloadException {
		try {
			// increment nb_download for this recipient
			RedisUtils.incrementNumberOfDownloadsForRecipient(redisManager, recipientId);
			String keyPli = RedisKeysEnum.FT_Download_Date.getKey(recipientId);
			LocalDateTime date = LocalDateTime.now();

			redisManager.saddString(keyPli, date.toString());
			// add to queue Redis download progress: to send download mail in progress to
			// the sender
			String downloadQueueValue = enclosureId + ":" + recipientId;
			redisManager.rpush(RedisQueueEnum.DOWNLOAD_QUEUE.getValue(), downloadQueueValue);
		} catch (Exception e) {
			throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue() + " : " + e.getMessage(), enclosureId, e);
		}
	}

	private List<FileRepresentation> getRootFiles(String enclosureId) throws DownloadException {
		List<FileRepresentation> rootFiles = new ArrayList<>();
		redisManager.lrange(RedisKeysEnum.FT_ROOT_FILES.getKey(enclosureId), 0, -1).forEach(rootFileName -> {
			String size = "";
			String hashRootFile = RedisUtils.generateHashsha1(enclosureId + ":" + rootFileName);
			try {
				size = redisManager.getHgetString(RedisKeysEnum.FT_ROOT_FILE.getKey(hashRootFile),
						RootFileKeysEnum.SIZE.getKey());
			} catch (Exception e) {
				throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), enclosureId, e);
			}
			FileRepresentation rootFile = new FileRepresentation();
			rootFile.setName(rootFileName);
			rootFile.setSize(Long.valueOf(size));
			rootFiles.add(rootFile);
			LOGGER.debug("root file: {}", rootFileName);
		});
		return rootFiles;
	}

	private List<DirectoryRepresentation> getRootDirs(String enclosureId) throws DownloadException {
		List<DirectoryRepresentation> rootDirs = new ArrayList<>();
		redisManager.lrange(RedisKeysEnum.FT_ROOT_DIRS.getKey(enclosureId), 0, -1).forEach(rootDirName -> {
			String size = "";
			String hashRootDir = RedisUtils.generateHashsha1(enclosureId + ":" + rootDirName);
			try {
				size = redisManager.getHgetString(RedisKeysEnum.FT_ROOT_DIR.getKey(hashRootDir),
						RootDirKeysEnum.TOTAL_SIZE.getKey());
			} catch (Exception e) {
				throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), enclosureId, e);
			}
			DirectoryRepresentation rootDir = new DirectoryRepresentation();
			rootDir.setName(rootDirName);
			rootDir.setTotalSize(Long.valueOf(size));
			rootDirs.add(rootDir);
			LOGGER.debug("root Dir: {}", rootDirName);
		});
		return rootDirs;
	}

	public void validatePublic(String enclosureId) throws UnauthorizedAccessException {
		Map<String, String> enclosureMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
		Boolean publicLink = Boolean.valueOf(enclosureMap.get(EnclosureKeysEnum.PUBLIC_LINK.getKey()));
		if (!publicLink) {
			throw new UnauthorizedAccessException("Unauthorized");
		}
	}

	public void validateToken(String enclosureId, String token) {
		Map<String, String> tokenMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ADMIN_TOKEN.getKey(enclosureId));
		if (tokenMap != null) {
			if (!token.equals(tokenMap.get(EnclosureKeysEnum.TOKEN.getKey()))) {
				throw new UnauthorizedAccessException("Invalid Token");
			}
		} else {
			throw new UnauthorizedAccessException("Invalid Token");
		}
	}

	private LocalDate validateDownloadAuthorizationPublic(String enclosureId)
			throws ExpirationEnclosureException, MetaloadException {
		LocalDate expirationDate = validateExpirationDate(enclosureId);
		return expirationDate;
	}
}

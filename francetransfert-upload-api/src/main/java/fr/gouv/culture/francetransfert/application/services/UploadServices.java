/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.services;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.model.PartETag;
import com.google.gson.Gson;

import fr.gouv.culture.francetransfert.application.error.ErrorEnum;
import fr.gouv.culture.francetransfert.application.error.UnauthorizedAccessException;
import fr.gouv.culture.francetransfert.application.resources.model.DeleteRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.DirectoryRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.EnclosureRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.FileInfoRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.FileRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.FranceTransfertDataRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.RecipientInfo;
import fr.gouv.culture.francetransfert.application.resources.model.ValidateCodeResponse;
import fr.gouv.culture.francetransfert.core.enums.EnclosureKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.FileKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RecipientKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisQueueEnum;
import fr.gouv.culture.francetransfert.core.enums.RootDirKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RootFileKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.SourceEnum;
import fr.gouv.culture.francetransfert.core.enums.StatutEnum;
import fr.gouv.culture.francetransfert.core.enums.ValidationErrorEnum;
import fr.gouv.culture.francetransfert.core.error.ApiValidationError;
import fr.gouv.culture.francetransfert.core.exception.ApiValidationException;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.exception.RetryStorageException;
import fr.gouv.culture.francetransfert.core.exception.StorageException;
import fr.gouv.culture.francetransfert.core.model.FormulaireContactData;
import fr.gouv.culture.francetransfert.core.model.NewRecipient;
import fr.gouv.culture.francetransfert.core.services.MimeService;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.services.StorageManager;
import fr.gouv.culture.francetransfert.core.utils.Base64CryptoService;
import fr.gouv.culture.francetransfert.core.utils.DateUtils;
import fr.gouv.culture.francetransfert.core.utils.RedisUtils;
import fr.gouv.culture.francetransfert.core.utils.StringUploadUtils;
import fr.gouv.culture.francetransfert.domain.exceptions.ExtensionNotFoundException;
import fr.gouv.culture.francetransfert.domain.exceptions.InvalidCaptchaException;
import fr.gouv.culture.francetransfert.domain.exceptions.UploadException;
import fr.gouv.culture.francetransfert.domain.utils.FileUtils;
import fr.gouv.culture.francetransfert.domain.utils.RedisForUploadUtils;

@Service
public class UploadServices {

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadServices.class);

	@Value("${enclosure.expire.days}")
	private int expiredays;

	@Value("${bucket.prefix}")
	private String bucketPrefix;

	@Value("${upload.limit}")
	private long uploadLimitSize;

	@Value("${upload.file.limit}")
	private long uploadFileLimitSize;

	@Value("${expire.token.sender}")
	private int daysToExpiretokenSender;

	@Autowired
	private RedisManager redisManager;

	@Value("${upload.expired.limit}")
	private int maxUpdateDate;

	@Value("${upload.limit.senderMail}")
	private Long maxUpload;

	@Value("${upload.token.chunkModulo:20}")
	private int chunkModulo;

	@Autowired
	private ConfirmationServices confirmationServices;

	@Autowired
	private StorageManager storageManager;

	@Autowired
	private Base64CryptoService base64CryptoService;

	@Autowired
	private StringUploadUtils stringUploadUtils;

	@Autowired
	private MimeService mimeService;

	@Autowired
	private CaptchaService captchaService;

	public DeleteRepresentation deleteFile(String enclosureId) {
		DeleteRepresentation deleteRepresentation = new DeleteRepresentation();
		try {
			String bucketName = RedisUtils.getBucketName(redisManager, enclosureId, bucketPrefix);
			String fileToDelete = storageManager.getZippedEnclosureName(enclosureId);
			Map<String, String> enclosureMap = redisManager
					.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
			enclosureMap.put(EnclosureKeysEnum.DELETED.getKey(), "true");
			redisManager.insertHASH(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId), enclosureMap);
			storageManager.deleteObject(bucketName, fileToDelete);
			redisManager.publishFT(RedisQueueEnum.DELETE_ENCLOSURE_QUEUE.getValue(), enclosureId);
			LOGGER.info("Fichier {} supprime", enclosureId);
			deleteRepresentation.setSuccess(redisManager.deleteKey(RedisKeysEnum.FT_ADMIN_TOKEN.getKey(enclosureId)));
			deleteRepresentation.setMessage("Fichier supprimé");
			deleteRepresentation.setStatus(HttpStatus.OK.value());
			return deleteRepresentation;
		} catch (Exception e) {
			LOGGER.error("Type: {} -- enclosureId: {} -- Message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(),
					enclosureId, e.getMessage(), e);
			deleteRepresentation.setMessage("Internal error, uuid: " + enclosureId);
			deleteRepresentation.setSuccess(false);
			deleteRepresentation.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			return deleteRepresentation;
		}
	}

	public EnclosureRepresentation updateExpiredTimeStamp(String enclosureId, LocalDate newDate) {
		try {
			Map<String, String> tokenMap = redisManager
					.hmgetAllString(RedisKeysEnum.FT_ADMIN_TOKEN.getKey(enclosureId));
			if (tokenMap != null) {
				Map<String, String> enclosureMap = redisManager
						.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
				enclosureMap.put(EnclosureKeysEnum.EXPIRED_TIMESTAMP.getKey(), newDate.atStartOfDay().toString());
				redisManager.insertHASH(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId), enclosureMap);
				return EnclosureRepresentation.builder().enclosureId(enclosureId).expireDate(newDate.toString())
						.build();
			} else {
				throw new UploadException("tokenMap from Redis is null", enclosureId);
			}
		} catch (Exception e) {
			throw new UploadException("Unable to update timestamp", enclosureId, e);
		}
	}

	public boolean processPrivateUpload(int flowChunkNumber, int flowTotalChunks, String flowIdentifier,
			MultipartFile multipartFile, String enclosureId, String senderId, String senderToken)
			throws MetaloadException, StorageException {

		try {

			redisManager.validateToken(senderId, senderToken);
			if ((flowChunkNumber % chunkModulo) == 0) {
				redisManager.extendTokenValidity(senderId, senderToken);
			}

			Boolean isUploaded = uploadFile(flowChunkNumber, flowTotalChunks, flowIdentifier, multipartFile,
					enclosureId, senderId);

			String upCountFile = redisManager.getHgetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
					EnclosureKeysEnum.UPLOAD_NB_FILES_DONE.getKey());
			if (StringUtils.isBlank(upCountFile)) {
				upCountFile = "0";
			}
			long uploadFilesCounter = Long.parseLong(upCountFile);
			if ((uploadFilesCounter % chunkModulo) == 0 && StringUtils.isNotBlank(senderToken)) {
				redisManager.extendTokenValidity(senderId, senderToken);
			}

			return isUploaded;
		} catch (ExtensionNotFoundException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error("Error while uploading enclosure " + enclosureId + " for chunk " + flowChunkNumber
					+ " and flowidentifier " + flowIdentifier + " : " + e.getMessage(), e);
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
					EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.ECH.getCode(), -1);
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
					EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.ECH.getWord(), -1);
//			try {
//				cleanEnclosure(enclosureId);
//			} catch (Exception e1) {
//				LOGGER.error("Error while cleanin after upload error : " + e.getMessage(), e);
//			}
			throw new UploadException(ErrorEnum.TECHNICAL_ERROR.getValue() + " during file upload : " + e.getMessage(),
					enclosureId, e);
		}
	}

	public boolean uploadFile(int flowChunkNumber, int flowTotalChunks, String flowIdentifier,
			MultipartFile multipartFile, String enclosureId, String senderId)
			throws MetaloadException, RetryStorageException, StorageException, IOException, ApiValidationException {

		checkExtension(multipartFile, enclosureId);

		String hashFid = RedisUtils.generateHashsha1(enclosureId + ":" + flowIdentifier);
		if (chunkExists(flowChunkNumber, enclosureId, flowIdentifier)) {
			return true; // multipart is uploaded
		}

		String bucketName = RedisUtils.getBucketName(redisManager, enclosureId, bucketPrefix);
		Map<String, String> redisFileInfo = RedisUtils.getFileInfo(redisManager, hashFid);
		if (redisFileInfo.isEmpty()) {
			ApiValidationError unknowFile = new ApiValidationError();
			unknowFile.setCodeChamp(ValidationErrorEnum.FT2019.getCodeChamp());
			unknowFile.setNumErreur(ValidationErrorEnum.FT2019.getNumErreur());
			unknowFile.setLibelleErreur(ValidationErrorEnum.FT2019.getLibelleErreur());
			throw new ApiValidationException(List.of(unknowFile), "Invalid File");
		}
		String fileNameWithPath = redisFileInfo.get(FileKeysEnum.REL_OBJ_KEY.getKey());

		if (RedisUtils.incrementCounterOfChunkIteration(redisManager, hashFid) == 1) {
			try {
				String uploadID = storageManager.generateUploadIdOsu(bucketName, fileNameWithPath);
				RedisForUploadUtils.addToFileMultipartUploadIdContainer(redisManager, uploadID, hashFid);
				redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
						EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.ECC.getCode(), -1);
				redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
						EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.ECC.getWord(), -1);
			} catch (Exception e) {
				redisManager.hsetString(RedisKeysEnum.FT_FILE.getKey(hashFid),
						FileKeysEnum.MUL_CHUNKS_ITERATION.getKey(), "0", -1);
				throw e;
			}

		}
		String uploadOsuId = RedisForUploadUtils.getUploadIdBlocking(redisManager, hashFid);

		boolean isUploaded = false;

		LOGGER.debug("Osu bucket name: {}", bucketName);
		PartETag partETag = storageManager.uploadMultiPartFileToOsuBucket(bucketName, flowChunkNumber, fileNameWithPath,
				multipartFile.getInputStream(), multipartFile.getSize(), uploadOsuId);
		String partETagToString = RedisForUploadUtils.addToPartEtags(redisManager, partETag, hashFid);
		LOGGER.debug("PartETag added {} for: {}", partETagToString, hashFid);
		isUploaded = true;
		long flowChuncksCounter = RedisUtils.incrementCounterOfUploadChunksPerFile(redisManager, hashFid);
		LOGGER.debug("FlowChuncksCounter in redis {}", flowChuncksCounter);
		LOGGER.info("Uploading File {} from enclosure {} - Chunk {}/{}", flowIdentifier, enclosureId,
				flowChuncksCounter, flowTotalChunks);
		if (flowTotalChunks >= flowChuncksCounter) {
			isUploaded = finishUploadFile(enclosureId, senderId, hashFid, bucketName, fileNameWithPath, uploadOsuId);
		}
		return isUploaded;
	}

	public boolean chunkExists(int flowChunkNumber, String enclosureId, String flowIdentifier) {
		String hashFid = RedisUtils.generateHashsha1(enclosureId + ":" + flowIdentifier);
		try {
			return RedisUtils.getNumberOfPartEtags(redisManager, hashFid).contains(flowChunkNumber);
		} catch (Exception e) {
			throw new UploadException("Checked chunk doest not exist : " + e.getMessage(), hashFid, e);
		}
	}

	/**
	 *
	 * @param metadata
	 * @param token
	 * @return
	 */
	public EnclosureRepresentation senderInfoWithTockenValidation(FranceTransfertDataRepresentation metadata,
			String token) {
		try {
			LOGGER.info("create metadata in redis with token validation {} / {} ", metadata.getSenderEmail(), token);
			/**
			 * Si l’expéditeur communique une adresse existante dans ignimission, l’envoi
			 * peut se faire sur une adresse externe ou en @email_valide_ignimission (Pas de
			 * règle nécessaire) Si l’expéditeur communique une adresse inexistante dans
			 * ignimission, l’envoi doit se faire exclusivement sur une adresse
			 * en @email_valide_ignimission. Si ce n’est pas le cas, un message d'erreur
			 * s’affiche.
			 **/
			boolean validSenderIgni = stringUploadUtils.isValidEmailIgni(metadata.getSenderEmail().toLowerCase());
			boolean validSender = stringUploadUtils.isValidEmail(metadata.getSenderEmail().toLowerCase());
			boolean validRecipients = false;
			boolean validRecipientsIgni = false;

			if (!validSender) {
				throw new UploadException(ErrorEnum.SENDER_MAIL_INVALID.getValue(), "Sender mail invalid");
			}

			if (!metadata.getPublicLink() && !CollectionUtils.isEmpty(metadata.getRecipientEmails())) {

				validRecipients = metadata.getRecipientEmails().stream().noneMatch(x -> {
					return !stringUploadUtils.isValidEmail(x);
				});
				validRecipientsIgni = metadata.getRecipientEmails().stream().noneMatch(x -> {
					return !stringUploadUtils.isValidEmailIgni(x);
				});

				if (!validRecipients) {
					throw new UploadException(ErrorEnum.RECIPIENT_MAIL_INVALID.getValue(), "Recipient mail invalid");
				}
			}

			LOGGER.debug("Can Upload ==> sender {} / recipients {}  ", validSenderIgni, validRecipients);

			if (!allowedSendermail(metadata.getSenderEmail().toLowerCase())) {
				throw new UploadException(ErrorEnum.SENDER_SEND_LIMIT.getValue(), "Sender send limit");
			}

			if (metadata.getMessage() != null && metadata.getLengthMessage() > 2500) {
				throw new UploadException(ErrorEnum.MESSAGE_LENGTH_LIMIT.getValue(), "Message length limit");
			}

			if ((validSenderIgni && metadata.getPublicLink())
					|| ((validSenderIgni || validRecipientsIgni) && validRecipients)) {
				String language = Locale.FRANCE.toString();
				// added
				if (metadata.getLanguage() != null) {
					language = metadata.getLanguage().toString();
				}
				metadata.setSource(SourceEnum.PRIVATE.getValue());

				boolean isRequiredToGeneratedCode = generateCode(metadata.getSenderEmail(), token, language);
				if (!isRequiredToGeneratedCode) {
					return createMetaDataEnclosureInRedis(metadata);
				}
			} else {
				LOGGER.error(
						"Invalid enclosure param for checkSender {} / checkRecipients {} for sender {} / recipients {}",
						validSenderIgni, validRecipients, metadata.getSenderEmail(), metadata.getRecipientEmails());
				return EnclosureRepresentation.builder().canUpload(false).build();
			}
			return null;
		} catch (Exception e) {
			String uuid = UUID.randomUUID().toString();
			throw new UploadException(
					ErrorEnum.TECHNICAL_ERROR.getValue() + " while checking mail creating meta : " + e.getMessage(),
					uuid, e);
		}
	}

	public FileInfoRepresentation getInfoPlis(String enclosureId) throws MetaloadException, UploadException {
		// validate Enclosure download right
		LocalDate expirationDate = validateDownloadExpiredAuthorizationPublic(enclosureId);
		LocalDate expirationArchiveDate = validateDownloadArchiveAuthorizationPublic(enclosureId);

		try {

			String passwordRedis = RedisUtils.getEnclosureValue(redisManager, enclosureId,
					EnclosureKeysEnum.PASSWORD.getKey());

			boolean withPassword = !StringUtils.isEmpty(passwordRedis);
			passwordRedis = "";

			boolean publicLink = Boolean.parseBoolean(
					RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.PUBLIC_LINK.getKey()));

			List<RecipientInfo> recipientsMails = new ArrayList<>();
			List<RecipientInfo> deletedRecipients = new ArrayList<>();

			int downloadCount = getSenderInfoPlis(enclosureId, recipientsMails, deletedRecipients);

			FileInfoRepresentation fileInfoRepresentation = infoPlis(enclosureId, expirationDate);
			fileInfoRepresentation.setDeletedRecipients(deletedRecipients);
			fileInfoRepresentation.setRecipientsMails(recipientsMails);
			fileInfoRepresentation.setPublicLink(publicLink);
			fileInfoRepresentation.setWithPassword(withPassword);
			fileInfoRepresentation.setDownloadCount(downloadCount);

			boolean archive = false;
			boolean expired = false;

			if (LocalDate.now().isAfter(expirationDate)) {
				expired = true;
				archive = true;
				fileInfoRepresentation.setArchiveUntilDate(expirationArchiveDate);
			}

			fileInfoRepresentation.setArchive(archive);
			fileInfoRepresentation.setExpired(expired);

			return fileInfoRepresentation;
		} catch (Exception e) {
			throw new UploadException(
					ErrorEnum.TECHNICAL_ERROR.getValue() + " while getting plisInfo : " + e.getMessage(), enclosureId,
					e);
		}
	}

	public FileInfoRepresentation getInfoPlisForReciever(String enclosureId, String recieverMail)
			throws MetaloadException, UploadException {
		// validate Enclosure download right
		LocalDate expirationDate = validateDownloadExpiredAuthorizationPublic(enclosureId);
		LocalDate expirationArchiveDate = validateDownloadArchiveAuthorizationPublic(enclosureId);

		Map<String, String> recList = RedisUtils.getRecipientsEnclosure(redisManager, enclosureId);
		boolean recipientInRedis = recList.containsKey(recieverMail);

		if (!recipientInRedis) {
			throw new UnauthorizedAccessException("Invalid Recipient");
		}

		try {

			List<RecipientInfo> recipientsMails = new ArrayList<>();

			int downloadCount = getRecieverInfoPli(enclosureId, recipientsMails, recieverMail);
			FileInfoRepresentation fileInfoRepresentation = infoPlis(enclosureId, expirationDate);
			fileInfoRepresentation.setRecipientsMails(recipientsMails);
			fileInfoRepresentation.setDownloadCount(downloadCount);

			boolean archive = false;
			boolean expired = false;

			if (LocalDate.now().isAfter(expirationDate)) {
				expired = true;
				archive = true;
				fileInfoRepresentation.setArchiveUntilDate(expirationArchiveDate);
			}

			fileInfoRepresentation.setArchive(archive);
			fileInfoRepresentation.setExpired(expired);
			return fileInfoRepresentation;
		} catch (Exception e) {
			throw new UploadException(
					ErrorEnum.TECHNICAL_ERROR.getValue() + " while getting plisInfo : " + e.getMessage(), enclosureId,
					e);
		}
	}

	public boolean resendDonwloadLink(String enclosureId, String email) {
		try {
			LOGGER.debug("create new recipient ");
			Map<String, String> recipientMap = RedisUtils.getRecipientsEnclosure(redisManager, enclosureId);
			boolean emailExist = recipientMap.containsKey(email.toLowerCase());
			if (!emailExist) {
				LOGGER.error("Recipient doesn't exist");
				throw new UploadException(ErrorEnum.RECIPIENT_DOESNT_EXIST.getValue());
			} else {
				NewRecipient rec = new NewRecipient();
				String idRecipient = RedisUtils.getRecipientId(redisManager, enclosureId, email);
				rec.setMail(email.toLowerCase());
				rec.setId(idRecipient);
				rec.setIdEnclosure(enclosureId);
				String recJsonInString = new Gson().toJson(rec);
				redisManager.publishFT(RedisQueueEnum.MAIL_NEW_RECIPIENT_QUEUE.getValue(), recJsonInString);
			}
			return true;
		} catch (Exception e) {
			throw new UploadException(
					ErrorEnum.TECHNICAL_ERROR.getValue() + " while resending download link : " + e.getMessage(),
					enclosureId, e);
		}
	}

	public boolean addNewRecipientToMetaDataInRedis(String enclosureId, String email) {
		try {
			LOGGER.debug("create new recipient ");

			boolean publicLink = Boolean.parseBoolean(
					RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.PUBLIC_LINK.getKey()));

			if (publicLink) {
				throw new UploadException("Cannot add user to public link");
			}

			Map<String, String> recipientMap = RedisUtils.getRecipientsEnclosure(redisManager, enclosureId);

			String sender = RedisUtils.getEmailSenderEnclosure(redisManager, enclosureId);

			boolean validSenderIgni = stringUploadUtils.isValidEmailIgni(sender);
			boolean recipientIgni = stringUploadUtils.isValidEmailIgni(email);

			if (!(validSenderIgni || recipientIgni)) {
				throw new UploadException("Invalid Sender/Recipient domain");
			}

			boolean emailExist = recipientMap.containsKey(email.toLowerCase());
			if (emailExist) {
				String recipientId = RedisUtils.getRecipientId(redisManager, enclosureId, email);
				Map<String, String> recipient = redisManager
						.hmgetAllString(RedisKeysEnum.FT_RECIPIENT.getKey(recipientId));

				recipient.put(RecipientKeysEnum.LOGIC_DELETE.getKey(), "0");
				redisManager.insertHASH(RedisKeysEnum.FT_RECIPIENT.getKey(recipientId), recipient);
				redisManager.saddString(RedisKeysEnum.FT_RECEIVE.getKey(email), enclosureId);
			} else {
				NewRecipient rec = new NewRecipient();
				String idRecipient = RedisForUploadUtils.createNewRecipient(redisManager, email.toLowerCase(),
						enclosureId);
				rec.setMail(email.toLowerCase());
				rec.setId(idRecipient);
				rec.setIdEnclosure(enclosureId);
				String recJsonInString = new Gson().toJson(rec);
				redisManager.publishFT(RedisQueueEnum.MAIL_NEW_RECIPIENT_QUEUE.getValue(), recJsonInString);
			}
			return true;
		} catch (Exception e) {
			throw new UploadException(
					ErrorEnum.TECHNICAL_ERROR.getValue() + " while adding new recipient : " + e.getMessage(),
					enclosureId, e);
		}
	}

	public boolean logicDeleteRecipient(String enclosureId, String email) throws MetaloadException {
		try {
			LOGGER.debug("delete recipient");
			String recipientId = RedisUtils.getRecipientId(redisManager, enclosureId, email);
			Map<String, String> recipientMap = redisManager
					.hmgetAllString(RedisKeysEnum.FT_RECIPIENT.getKey(recipientId));

			recipientMap.put(RecipientKeysEnum.LOGIC_DELETE.getKey(), "1");
			redisManager.insertHASH(RedisKeysEnum.FT_RECIPIENT.getKey(recipientId), recipientMap);
			redisManager.srem(RedisKeysEnum.FT_RECEIVE.getKey(email), enclosureId);
			return true;

		} catch (Exception e) {
			throw new UploadException(
					ErrorEnum.TECHNICAL_ERROR.getValue() + " while deleting recipient : " + e.getMessage(), email, e);
		}

	}

	public EnclosureRepresentation createMetaDataEnclosureInRedis(FranceTransfertDataRepresentation metadata) {
		if (FileUtils.getEnclosureTotalSize(metadata.getRootFiles(), metadata.getRootDirs()) > uploadLimitSize
				|| FileUtils.getSizeFileOver(metadata.getRootFiles(), uploadFileLimitSize)) {
			LOGGER.error("enclosure size > upload limit size: {}", uploadLimitSize);
			throw new UploadException(ErrorEnum.LIMT_SIZE_ERROR.getValue());
		}
		try {
			LOGGER.debug("limit enclosure size is < upload limit size: {}", uploadLimitSize);
			// generate password if provided one not valid
			if (metadata.getPassword() == null) {
				LOGGER.info("password is null");
			}
			if (StringUtils.isNotBlank(metadata.getPassword())
					&& base64CryptoService.validatePassword(metadata.getPassword().trim())) {
				LOGGER.info("Hashing password");
				String passwordHashed = base64CryptoService.aesEncrypt(metadata.getPassword().trim());
				metadata.setPassword(passwordHashed);
				metadata.setPasswordGenerated(false);
				LOGGER.debug("calculate pasword hashed ******");
				passwordHashed = "";
			} else {
				LOGGER.info("No password generating new one");
				String generatedPassword = base64CryptoService.generatePassword(0);
				LOGGER.debug("Hashing generated password");
				String passwordHashed = base64CryptoService.aesEncrypt(generatedPassword.trim());
				metadata.setPassword(passwordHashed);
				metadata.setPasswordGenerated(true);
				passwordHashed = "";
			}
			LOGGER.debug("create enclosure metadata in redis ");
			HashMap<String, String> hashEnclosureInfo = RedisForUploadUtils.createHashEnclosure(redisManager, metadata);

			LOGGER.debug("get expiration date and enclosure id back ");
			String enclosureId = hashEnclosureInfo.get(RedisForUploadUtils.ENCLOSURE_HASH_GUID_KEY);
			String expireDate = hashEnclosureInfo.get(RedisForUploadUtils.ENCLOSURE_HASH_EXPIRATION_DATE_KEY);

			LOGGER.debug("update list date-enclosure in redis ");
			RedisUtils.updateListOfDatesEnclosure(redisManager, enclosureId);
			LOGGER.debug("create sender metadata in redis");
			String senderId = RedisForUploadUtils.createHashSender(redisManager, metadata, enclosureId);
			LOGGER.debug("create all recipients metadata in redis ");
			RedisForUploadUtils.createAllRecipient(redisManager, metadata, enclosureId);
			LOGGER.debug("create root-files metadata in redis ");
			RedisForUploadUtils.createRootFiles(redisManager, metadata, enclosureId);
			LOGGER.debug("create root-dirs metadata in redis ");
			RedisForUploadUtils.createRootDirs(redisManager, metadata, enclosureId);
			LOGGER.debug("create contents-files-ids metadata in redis ");
			RedisForUploadUtils.createContentFilesIds(redisManager, metadata, enclosureId);
			LOGGER.info("enclosure id : {} and the sender id : {} and senderMail : {}", enclosureId, senderId,
					metadata.getSenderEmail());
			RedisForUploadUtils.createDeleteToken(redisManager, enclosureId);

			return EnclosureRepresentation.builder().enclosureId(enclosureId).senderId(senderId).expireDate(expireDate)
					.canUpload(Boolean.TRUE).build();
		} catch (Exception e) {
			String uuid = UUID.randomUUID().toString();
			throw new UploadException("Error generating Metadata", uuid, e);
		}
	}

	public Boolean validateMailDomain(List<String> mails) {
		Boolean isValid = false;
		isValid = mails.stream().allMatch(mail -> {
			if (stringUploadUtils.isValidEmail(mail)) {
				return stringUploadUtils.isValidEmailIgni(mail);
			}
			return false;
		});

		return isValid;
	}

	public Boolean allowedSendermail(String senderMail) {
		if (!stringUploadUtils.isValidEmailIgni(senderMail)) {
			Long nbUpload = numberTokensOfTheDay(senderMail);
			LOGGER.debug("Upload for user {} = {}", senderMail, nbUpload);
			if (nbUpload >= maxUpload) {
				return false;
			}
			return true;
		}
		return true;
	}

	public List<FileInfoRepresentation> getSenderPlisList(ValidateCodeResponse metadata) throws MetaloadException {
		List<FileInfoRepresentation> listPlis = new ArrayList<FileInfoRepresentation>();
		List<String> result = RedisUtils.getSentPli(redisManager, metadata.getSenderMail());

		if (!CollectionUtils.isEmpty(result)) {
			for (String enclosureId : result) {
				try {
					FileInfoRepresentation enclosureInfo = getInfoPlis(enclosureId);
					if (!enclosureInfo.isDeleted()) {
						listPlis.add(enclosureInfo);
					}
				} catch (Exception e) {
					LOGGER.error("Cannot get plis {} for list ", enclosureId, e);
				}
			}
		}
		return listPlis;
	}

	public List<FileInfoRepresentation> getReceivedPlisList(ValidateCodeResponse metadata) throws MetaloadException {
		List<FileInfoRepresentation> listPlis = new ArrayList<FileInfoRepresentation>();
		List<String> result = RedisUtils.getReceivedPli(redisManager, metadata.getSenderMail());

		if (!CollectionUtils.isEmpty(result)) {
			for (String enclosureId : result) {
				try {
					FileInfoRepresentation enclosureInfo = getInfoPlisForReciever(enclosureId,
							metadata.getSenderMail());
					if (!enclosureInfo.isDeleted()) {
						listPlis.add(enclosureInfo);
					}
				} catch (Exception e) {
					LOGGER.error("Cannot get plis {} for list ", enclosureId, e);
				}
			}
		}
		return listPlis;
	}

	public LocalDate validateExpirationDate(String enclosureId) throws MetaloadException {
		LocalDate expirationDate = DateUtils.convertStringToLocalDate(
				RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.EXPIRED_TIMESTAMP.getKey()));
		if (LocalDate.now().isAfter(expirationDate)) {
			throw new UploadException("Vous ne pouvez plus accéder à ces fichiers", enclosureId);
		}
		return expirationDate;
	}

	/**
	 *
	 * @param metadata
	 * @return
	 */
	public boolean senderContact(FormulaireContactData metadata) {

		if (!captchaService.checkCaptcha(metadata.getChallengeId(), metadata.getUserResponse(),
				metadata.getCaptchaType())) {
			throw new InvalidCaptchaException("Captcha incorrect");
		}
		checkNull(metadata);
		String jsonInString = new Gson().toJson(metadata);
		redisManager.publishFT(RedisQueueEnum.FORMULE_CONTACT_QUEUE.getValue(), jsonInString);
		return true;
	}

	public void cleanEnclosure(String prefix) throws MetaloadException, RetryStorageException {
		String bucketName = RedisUtils.getBucketName(redisManager, prefix, bucketPrefix);
		storageManager.deleteFilesWithPrefix(bucketName, prefix);
	}

	public boolean logout(ValidateCodeResponse data) throws MetaloadException {
		redisManager.validateToken(data.getSenderMail(), data.getSenderToken());
		redisManager.expireToken(data.getSenderMail(), data.getSenderToken());
		return true;
	}

	public RecipientInfo buildRecipient(String email, String enclosureId) throws MetaloadException {
		String recipientId = RedisUtils.getRecipientId(redisManager, enclosureId, email);
		Set<String> downloadDate = redisManager.smembersString(RedisKeysEnum.FT_Download_Date.getKey(recipientId));
		ArrayList<String> downloadDates = new ArrayList<String>(downloadDate);
		int nbDownload = RedisUtils.getNumberOfDownloadsPerRecipient(redisManager, recipientId);
		boolean deleted = RedisUtils.isRecipientDeleted(redisManager, recipientId);
		RecipientInfo recipient = new RecipientInfo(email, nbDownload, deleted, downloadDates);
		return recipient;
	}

	private void checkNull(FormulaireContactData metadat) {
		if (StringUtils.isBlank(metadat.getNom())) {
			metadat.setNom("");
		}
		if (StringUtils.isBlank(metadat.getPrenom())) {
			metadat.setPrenom("");
		}
		if (StringUtils.isBlank(metadat.getAdministration())) {
			metadat.setAdministration("");
		}
		if (StringUtils.isBlank(metadat.getSubject())) {
			metadat.setSubject("");
		}
	}

	private boolean finishUploadFile(String enclosureId, String senderId, String hashFid, String bucketName,
			String fileNameWithPath, String uploadOsuId) throws StorageException, MetaloadException {
		List<PartETag> partETags = RedisForUploadUtils.getPartEtags(redisManager, hashFid);
		String succesUpload = storageManager.completeMultipartUpload(bucketName, fileNameWithPath, uploadOsuId,
				partETags);
		boolean isUpload = false;
		if (succesUpload != null) {
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
					EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.ECC.getCode(), -1);
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
					EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.ECC.getWord(), -1);
			int fileCount = RedisUtils.getFilesIds(redisManager, enclosureId).size();
			long uploadFilesCounter = RedisUtils.incrementCounterOfUploadFilesEnclosure(redisManager, enclosureId);
			LOGGER.info("Finish upload File {}/{} for enclosure {} ==> {} ", uploadFilesCounter, fileCount, enclosureId,
					fileNameWithPath);
			if (fileCount == uploadFilesCounter) {
				redisManager.publishFT(RedisQueueEnum.ZIP_QUEUE.getValue(), enclosureId);
				RedisUtils.addPliToDay(redisManager, senderId, enclosureId);
				LOGGER.info("Finish upload enclosure ==> {} ", enclosureId);
				redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
						EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.CHT.getCode(), -1);
				redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
						EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.CHT.getWord(), -1);
			}
			isUpload = true;
		} else {
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
					EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.ECH.getCode(), -1);
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
					EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.ECH.getWord(), -1);
			isUpload = false;
		}
		return isUpload;
	}

	private void checkExtension(MultipartFile multipartFile, String enclosureId)
			throws MetaloadException, RetryStorageException {
		if (!mimeService.isAuthorisedMimeTypeFromFileName(multipartFile.getOriginalFilename())) {
			LOGGER.error("Extension file no authorised for file {}", multipartFile.getOriginalFilename());
			cleanEnclosure(enclosureId);
			throw new ExtensionNotFoundException(
					"Extension file no authorised for file " + multipartFile.getOriginalFilename());
		}
		LOGGER.debug("Extension file authorised");
	}

	private int getSenderInfoPlis(String enclosureId, List<RecipientInfo> recipientsMails,
			List<RecipientInfo> deletedRecipients) throws MetaloadException {
		for (Map.Entry<String, String> recipient : RedisUtils.getRecipientsEnclosure(redisManager, enclosureId)
				.entrySet()) {
			RecipientInfo recinfo = buildRecipient(recipient.getKey(), enclosureId);
			if (recinfo.isDeleted()) {
				deletedRecipients.add(recinfo);
			} else {
				recipientsMails.add(recinfo);
			}
		}
		int downloadCount = 0;
		String downString = getNumberOfDownloadPublic(enclosureId);
		if (StringUtils.isNotBlank(downString)) {
			downloadCount = Integer.parseInt(getNumberOfDownloadPublic(enclosureId));
		}
		return downloadCount;
	}

	private int getRecieverInfoPli(String enclosureId, List<RecipientInfo> recipientsMails, String recipient)
			throws MetaloadException {

		RecipientInfo recinfo = buildRecipient(recipient, enclosureId);
		if (!recinfo.isDeleted()) {
			recipientsMails.add(recinfo);
		}

		int downloadCount = 0;
		String downString = getNumberOfDownloadPublic(enclosureId);
		if (StringUtils.isNotBlank(downString)) {
			downloadCount = Integer.parseInt(getNumberOfDownloadPublic(enclosureId));
		}
		return downloadCount;
	}

	private LocalDate validateDownloadArchiveAuthorizationPublic(String enclosureId)
			throws MetaloadException, UploadException {

		LocalDate archiveDate;
		String dateString = RedisUtils.getEnclosureValue(redisManager, enclosureId,
				EnclosureKeysEnum.EXPIRED_TIMESTAMP_ARCHIVE.getKey());

		archiveDate = DateUtils.convertStringToLocalDate(dateString);
		if (LocalDate.now().isAfter(archiveDate) && StringUtils.isNotBlank(dateString)) {
			throw new UploadException("Vous ne pouvez plus modifier ces fichiers archivés", enclosureId);
		}

		boolean deleted = Boolean.parseBoolean(
				RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.DELETED.getKey()));
		if (deleted) {
			throw new UploadException("Vous ne pouvez plus accéder à ces fichiers", enclosureId);
		}
		return archiveDate;
	}

	private LocalDate validateDownloadExpiredAuthorizationPublic(String enclosureId)
			throws MetaloadException, UploadException {
		LocalDate expirationDate = DateUtils.convertStringToLocalDate(
				RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.EXPIRED_TIMESTAMP.getKey()));
		boolean deleted = Boolean.parseBoolean(
				RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.DELETED.getKey()));
		if (deleted) {
			throw new UploadException("Vous ne pouvez plus accéder à ces fichiers", enclosureId);
		}
		return expirationDate;
	}

	private FileInfoRepresentation infoPlis(String enclosureId, LocalDate expirationDate) throws MetaloadException {
		String message = RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.MESSAGE.getKey());

		String subject = RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.SUBJECT.getKey());

		boolean deleted = Boolean.parseBoolean(
				RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.DELETED.getKey()));

		String senderMail = RedisUtils.getEmailSenderEnclosure(redisManager, enclosureId);
		List<FileRepresentation> rootFiles = getRootFiles(enclosureId);
		List<DirectoryRepresentation> rootDirs = getRootDirs(enclosureId);
		Map<String, String> enclosureMap = redisManager
				.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey((enclosureId)));
		String timestamp = enclosureMap.get(EnclosureKeysEnum.TIMESTAMP.getKey());

		long tailleLong = (long) RedisUtils.getTotalSizeEnclosure(redisManager, enclosureId);
		String tailleStr = org.apache.commons.io.FileUtils.byteCountToDisplaySize(tailleLong);

		FileInfoRepresentation fileInfoRepresentation = FileInfoRepresentation.builder().validUntilDate(expirationDate)
				.senderEmail(senderMail).message(message).rootFiles(rootFiles).rootDirs(rootDirs).timestamp(timestamp)
				.subject(subject).deleted(deleted).enclosureId(enclosureId).totalSizeLong(tailleLong)
				.totalSize(tailleStr).build();
		return fileInfoRepresentation;
	}

	private boolean generateCode(String senderMail, String token, String currentLanguage) {
		boolean result = false;
		if (stringUploadUtils.isValidEmail(senderMail)) {
			try {
				senderMail = senderMail.toLowerCase();
				// verify token in redis
				if (!StringUtils.isEmpty(token)) {
					try {
						LOGGER.info("verify token from sender in redis", senderMail);
						redisManager.validateToken(senderMail, token);
						redisManager.extendTokenValidity(senderMail, token);
					} catch (Exception e) {
						confirmationServices.generateCodeConfirmation(senderMail, currentLanguage);
						result = true;
						LOGGER.info("generate confirmation code for sender mail {}", senderMail);
					}
				} else {
					LOGGER.debug("token does not exist");
					confirmationServices.generateCodeConfirmation(senderMail, currentLanguage);
					result = true;
					LOGGER.info("generate confirmation code for sender mail {}", senderMail);
				}
				return result;
			} catch (Exception e) {
				String uuid = UUID.randomUUID().toString();
				throw new UploadException(ErrorEnum.TECHNICAL_ERROR.getValue() + " generating code : " + e.getMessage(),
						uuid, e);
			}
		} else {
			throw new UploadException(ErrorEnum.SENDER_MAIL_INVALID.getValue(), "Invalid sendermail");
		}
	}

	private List<FileRepresentation> getRootFiles(String enclosureId) {
		List<FileRepresentation> rootFiles = new ArrayList<>();
		redisManager.lrange(RedisKeysEnum.FT_ROOT_FILES.getKey(enclosureId), 0, -1).forEach(rootFileName -> {
			String size = "";
			String hashRootFile = RedisUtils.generateHashsha1(enclosureId + ":" + rootFileName);
			try {
				size = redisManager.getHgetString(RedisKeysEnum.FT_ROOT_FILE.getKey(hashRootFile),
						RootFileKeysEnum.SIZE.getKey());
			} catch (Exception e) {
				throw new UploadException("Cannot get RootFiles : " + e.getMessage(), enclosureId, e);
			}
			FileRepresentation rootFile = new FileRepresentation();
			rootFile.setName(rootFileName);
			rootFile.setSize(Long.valueOf(size));
			rootFiles.add(rootFile);
			LOGGER.debug("root file: {}", rootFileName);
		});
		return rootFiles;
	}

	private List<DirectoryRepresentation> getRootDirs(String enclosureId) {
		List<DirectoryRepresentation> rootDirs = new ArrayList<>();
		redisManager.lrange(RedisKeysEnum.FT_ROOT_DIRS.getKey(enclosureId), 0, -1).forEach(rootDirName -> {
			String size = "";
			String hashRootDir = RedisUtils.generateHashsha1(enclosureId + ":" + rootDirName);
			try {
				size = redisManager.getHgetString(RedisKeysEnum.FT_ROOT_DIR.getKey(hashRootDir),
						RootDirKeysEnum.TOTAL_SIZE.getKey());
			} catch (Exception e) {
				throw new UploadException("Cannot get RootDirs : " + e.getMessage(), enclosureId, e);
			}
			DirectoryRepresentation rootDir = new DirectoryRepresentation();
			rootDir.setName(rootDirName);
			rootDir.setTotalSize(Long.valueOf(size));
			rootDirs.add(rootDir);
			LOGGER.debug("root Dir: {}", rootDirName);
		});
		return rootDirs;
	}

	private LocalDate validateDownloadAuthorizationPublic(String enclosureId)
			throws MetaloadException, UploadException {
		LocalDate expirationDate = validateExpirationDate(enclosureId);
		boolean deleted = Boolean.parseBoolean(
				RedisUtils.getEnclosureValue(redisManager, enclosureId, EnclosureKeysEnum.DELETED.getKey()));
		if (deleted) {
			throw new UploadException("Vous ne pouvez plus accéder à ces fichiers", enclosureId);
		}
		return expirationDate;
	}

	private String getNumberOfDownloadPublic(String enclosureId) {
		Map<String, String> enclosureMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
		if (enclosureMap != null) {
			return enclosureMap.get(EnclosureKeysEnum.PUBLIC_DOWNLOAD_COUNT.getKey());
		} else {
			throw new UploadException("Error getting public donwload count", enclosureId);
		}
	}

	private Long numberTokensOfTheDay(String senderMail) {
		Set<String> setTokenInRedis = redisManager.smembersString(RedisKeysEnum.FT_SENDER_PLIS.getKey(senderMail));
		if (CollectionUtils.isNotEmpty(setTokenInRedis)) {
			return setTokenInRedis.stream().count();
		}
		return 0L;
	}

	private boolean isBoolean(String value) {
		return value != null
				&& Arrays.stream(new String[] { "true", "false", "1", "0" }).anyMatch(b -> b.equalsIgnoreCase(value));
	}

}

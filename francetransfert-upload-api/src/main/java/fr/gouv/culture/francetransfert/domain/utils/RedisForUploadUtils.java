/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.domain.utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.redisson.client.RedisTryAgainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.amazonaws.services.s3.model.PartETag;

import fr.gouv.culture.francetransfert.application.resources.model.FranceTransfertDataRepresentation;
import fr.gouv.culture.francetransfert.core.enums.EnclosureKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.FileKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RecipientKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RootDirKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RootFileKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.SenderKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.StatutEnum;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.utils.RedisUtils;
import fr.gouv.culture.francetransfert.domain.exceptions.UploadException;
import fr.gouv.culture.francetransfert.domain.redis.entity.FileDomain;

@Service
public class RedisForUploadUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(RedisForUploadUtils.class);
	public final static String ENCLOSURE_HASH_GUID_KEY = "guidEnclosure";
	public final static String ENCLOSURE_HASH_EXPIRATION_DATE_KEY = "expirationDate";

	private static int maxUpdateDate;

	@Value("${upload.expired.limit}")
	public void setMaxUpdateDate(int maxUpdateDate) {
		RedisForUploadUtils.maxUpdateDate = maxUpdateDate;
	}

	public static HashMap<String, String> createHashEnclosure(RedisManager redisManager,
			FranceTransfertDataRepresentation metadata) {
		// ================ set enclosure info in redis ================
		HashMap<String, String> hashEnclosureInfo = new HashMap<String, String>();
		String guidEnclosure = "";

		try {
			guidEnclosure = RedisUtils.generateGUID();
			LOGGER.debug("enclosure id : {}", guidEnclosure);

			Map<String, String> map = new HashMap<>();

			LocalDateTime startDate = LocalDateTime.now();
			LOGGER.debug("enclosure creation date: {}", startDate);
			map.put(EnclosureKeysEnum.TIMESTAMP.getKey(), startDate.toString());

			LocalDateTime expiredDate = getExpiredTimeStamp(metadata.getExpireDelay());
			LOGGER.debug("enclosure expire date: {}", expiredDate);
			map.put(EnclosureKeysEnum.EXPIRED_TIMESTAMP.getKey(), expiredDate.toString());

			LOGGER.debug("password: *******");
			map.put(EnclosureKeysEnum.PASSWORD.getKey(), metadata.getPassword());

			LOGGER.debug("is password generated! : {}", metadata.getPasswordGenerated());
			map.put(EnclosureKeysEnum.PASSWORD_GENERATED.getKey(), metadata.getPasswordGenerated().toString());

			String result = metadata.getLanguage().toString();
			Pattern pattern = Pattern.compile("-");
			String[] items = pattern.split(result, 2);

			result = items[0];

			LOGGER.debug("enclosure language: {}", result);
			map.put(EnclosureKeysEnum.LANGUAGE.getKey(), result);

			LOGGER.debug("is password zip checked? : {}", metadata.getZipPassword().toString());
			map.put(EnclosureKeysEnum.PASSWORD_ZIP.getKey(), metadata.getZipPassword().toString());
			LOGGER.debug("enclosure ID pli: {}", guidEnclosure);

			if (!StringUtils.isBlank(metadata.getMessage())) {
				LOGGER.debug("message: {}",
						StringUtils.isEmpty(metadata.getMessage()) ? "is empty" : metadata.getMessage());
				map.put(EnclosureKeysEnum.MESSAGE.getKey(), metadata.getMessage());
			} else {
				map.put(EnclosureKeysEnum.MESSAGE.getKey(), "");
			}
			LOGGER.debug("Objet du pli : {}", metadata.getSubject());
			if (!StringUtils.isBlank(metadata.getSubject())) {
				LOGGER.debug("objet: {}",
						StringUtils.isEmpty(metadata.getMessage()) ? "is empty" : metadata.getSubject());
				map.put(EnclosureKeysEnum.SUBJECT.getKey(), metadata.getSubject());
			} else {
				map.put(EnclosureKeysEnum.SUBJECT.getKey(), "");
			}
			LOGGER.debug("hashFile null for now : {}");
			map.put(EnclosureKeysEnum.HASH_FILE.getKey(), "");
			LOGGER.debug("Public Link : {}", metadata.getPublicLink());
			map.put(EnclosureKeysEnum.PUBLIC_LINK.getKey(), metadata.getPublicLink().toString());
			LOGGER.debug("Create Public Link Download Count");
			map.put(EnclosureKeysEnum.PUBLIC_DOWNLOAD_COUNT.getKey(), "0");
			// ---
			map.put(EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.INI.getCode());
			map.put(EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.INI.getWord());

			map.put(EnclosureKeysEnum.SOURCE.getKey(), metadata.getSource());
			map.put(EnclosureKeysEnum.ENVOIMDPDEST.getKey(), Boolean.toString(metadata.isEnvoiMdpDestinataires()));

			redisManager.insertHASH(RedisKeysEnum.FT_ENCLOSURE.getKey(guidEnclosure), map);

			hashEnclosureInfo.put(ENCLOSURE_HASH_GUID_KEY, guidEnclosure);
			hashEnclosureInfo.put(ENCLOSURE_HASH_EXPIRATION_DATE_KEY, expiredDate.toLocalDate().toString());
			return hashEnclosureInfo;
		} catch (Exception e) {
			throw new UploadException("Error inserting metadata : " + e.getMessage(), e);
		}
	}

	public static String createHashSender(RedisManager redisManager, FranceTransfertDataRepresentation metadata,
			String enclosureId) {
		// ================ set sender info in redis ================
		try {
			if (null == metadata.getSenderEmail()) {
				throw new UploadException("Sender null", enclosureId);
			}
			boolean isNewSender = StringUtils.isEmpty(metadata.getConfirmedSenderId());
			if (isNewSender) {
				metadata.setConfirmedSenderId(RedisUtils.generateGUID());
			}
			Map<String, String> map = new HashMap<>();
			map.put(SenderKeysEnum.EMAIL.getKey(), metadata.getSenderEmail());
			LOGGER.debug("sender mail: {}", metadata.getSenderEmail());
			map.put(SenderKeysEnum.IS_NEW.getKey(), isNewSender ? "0" : "1");
			LOGGER.debug("is new sender: {}", isNewSender ? "0" : "1");
			map.put(SenderKeysEnum.ID.getKey(), metadata.getConfirmedSenderId());
			LOGGER.debug("sender id: {}", metadata.getConfirmedSenderId());
			redisManager.insertHASH(RedisKeysEnum.FT_SENDER.getKey(enclosureId), map);
			return metadata.getConfirmedSenderId();
		} catch (Exception e) {
			throw new UploadException("Error creating sender : " + e.getMessage(), enclosureId, e);
		}
	}

	public static void createAllRecipient(RedisManager redisManager, FranceTransfertDataRepresentation metadata,
			String enclosureId) {
		try {

			if (Boolean.FALSE.equals(metadata.getPublicLink())) {
				if (CollectionUtils.isEmpty(metadata.getRecipientEmails())) {
					throw new UploadException("Empty recipient", enclosureId);
				}
				Map<String, String> mapRecipients = new HashMap<>();
				metadata.getRecipientEmails().forEach(recipientMail -> {
					String guidRecipient = RedisUtils.generateGUID();
					mapRecipients.put(recipientMail.toLowerCase(), guidRecipient);

					// idRecepient => HASH { nbDl: "0" }
					Map<String, String> mapRecipient = new HashMap<>();
					mapRecipient.put(RecipientKeysEnum.NB_DL.getKey(), "0");
					mapRecipient.put(RecipientKeysEnum.PASSWORD_TRY_COUNT.getKey(), "0");
					mapRecipient.put(RecipientKeysEnum.LOGIC_DELETE.getKey(), "0");
					redisManager.insertHASH(RedisKeysEnum.FT_RECIPIENT.getKey(guidRecipient), mapRecipient);
					// redisManager.insertHASH(RedisKeysEnum.FT_Download_Date.getKey(guidRecipient),
					// mapRecipients);
					LOGGER.debug("mail_recepient : {} => recepient id: {}", recipientMail, guidRecipient);
				});
				// enclosure:enclosureId:recipients:emails-ids => HASH <mail_recepient,
				// idRecepient>
				redisManager.insertHASH(RedisKeysEnum.FT_RECIPIENTS.getKey(enclosureId), mapRecipients);

			}
		} catch (Exception e) {
			throw new UploadException("Error creating recipient : " + e.getMessage(), enclosureId, e);
		}
	}

	public static String createNewRecipient(RedisManager redisManager, String email, String enclosureId) {
		try {

			if (StringUtils.isBlank(email)) {
				throw new UploadException("Empty recipient", enclosureId);
			}
			Map<String, String> mapRecipients = new HashMap<>();

			String guidRecipient = RedisUtils.generateGUID();
			mapRecipients.put(email, guidRecipient);
			// idRecepient => HASH { nbDl: "0" }
			Map<String, String> mapRecipient = new HashMap<>();
			mapRecipient.put(RecipientKeysEnum.NB_DL.getKey(), "0");
			mapRecipient.put(RecipientKeysEnum.PASSWORD_TRY_COUNT.getKey(), "0");
			mapRecipient.put(RecipientKeysEnum.LOGIC_DELETE.getKey(), "0");
			redisManager.insertHASH(RedisKeysEnum.FT_RECIPIENT.getKey(guidRecipient), mapRecipient);
			LOGGER.debug("mail_recepient : {} => recepient id: {}", email, guidRecipient);
			// enclosure:enclosureId:recipients:emails-ids => HASH <mail_recepient,
			// idRecepient>

			// redisManager.insertHASH(RedisKeysEnum.FT_Download_Date.getKey(guidRecipient),
			// mapRecipients);
			redisManager.insertHASH(RedisKeysEnum.FT_RECIPIENTS.getKey(enclosureId), mapRecipients);
			return guidRecipient;
		} catch (Exception e) {
			throw new UploadException("Error creating recipient : " + e.getMessage(), enclosureId, e);
		}
	}

	public static void createRootFiles(RedisManager redisManager, FranceTransfertDataRepresentation metadata,
			String enclosureId) {
		try {
			Map<String, String> filesMap = FileUtils.searchRootFiles(metadata);
			// ================ set List root-files info in redis================
			redisManager.insertList( // idRootFilesNames => LIST [file1, file2, ...]
					RedisKeysEnum.FT_ROOT_FILES.getKey(enclosureId), new ArrayList(filesMap.keySet()));

			for (Map.Entry<String, String> currentFile : filesMap.entrySet()) {
				// ================ set HASH root-file info in redis================
				Map<String, String> map = new HashMap<>();
				map.put(RootFileKeysEnum.SIZE.getKey(), currentFile.getValue());
				LOGGER.debug(" root file: {} => size {}", currentFile.getKey(), currentFile.getValue());
				redisManager.insertHASH(RedisKeysEnum.FT_ROOT_FILE
						.getKey(RedisUtils.generateHashsha1(enclosureId + ":" + currentFile.getKey())), map);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	public static void createDeleteToken(RedisManager redisManager, String enclosureId) {
		try {
			Map<String, String> mapToken = new HashMap<>();
			mapToken.put("token", UUID.randomUUID().toString());
			redisManager.insertHASH(RedisKeysEnum.FT_ADMIN_TOKEN.getKey(enclosureId), mapToken);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	public static void createRootDirs(RedisManager redisManager, FranceTransfertDataRepresentation metadata,
			String enclosureId) {
		try {
			Map<String, String> dirsMap = FileUtils.searchRootDirs(metadata);
			// ================ set List root-dirs info in redis================
			redisManager.insertList( // idRootDirsNames => LIST [dir1, dir2, ...]
					RedisKeysEnum.FT_ROOT_DIRS.getKey(enclosureId), new ArrayList(dirsMap.keySet()));

			for (Map.Entry<String, String> currentDir : dirsMap.entrySet()) {
				// ================ set HASH root-dir info in redis================
				Map<String, String> map = new HashMap<>();
				map.put(RootDirKeysEnum.TOTAL_SIZE.getKey(), currentDir.getValue());
				LOGGER.debug(" root dir: {} => total size {}", currentDir.getKey(), currentDir.getValue());
				redisManager.insertHASH(RedisKeysEnum.FT_ROOT_DIR
						.getKey(RedisUtils.generateHashsha1(enclosureId + ":" + currentDir.getKey())), map);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	public static void createContentFilesIds(RedisManager redisManager, FranceTransfertDataRepresentation metadata,
			String enclosureId) {
		try {
			List<FileDomain> files = FileUtils.searchFiles(metadata, enclosureId);
			// ================ set List files info in redis================
			redisManager.insertList( // FILES_IDS => list [ SHA1(enclosureId":"fid1), SHA1(enclosureId":"fid2), ...]
					RedisKeysEnum.FT_FILES_IDS.getKey(enclosureId),
					files.stream().map(file -> RedisUtils.generateHashsha1(enclosureId + ":" + file.getFid()))
							.collect(Collectors.toList()));
			for (FileDomain currentfile : files) {
				LOGGER.debug(" current file: {} =>  size {}", currentfile.getFid(), currentfile.getSize());
				String shaFid = RedisUtils.generateHashsha1(enclosureId + ":" + currentfile.getFid());

				// create list part-etags for each file in Redis =
				// file:SHA1(GUID_pli:fid):mul:part-etags =>List
				// [etag1.getPartNumber()+":"+etag1.getETag(),
				// etag2.getPartNumber()+":"+etag2.getETag(), ...]
				LOGGER.debug(" create list part-etags in redis ");
				RedisUtils.createListPartEtags(redisManager, shaFid);
				RedisUtils.createListIdContainer(redisManager, shaFid);
				// ================ set HASH file info in redis================
				Map<String, String> map = new HashMap<>();
				map.put(FileKeysEnum.REL_OBJ_KEY.getKey(), currentfile.getPath());
				LOGGER.debug(" current file path : {} ", currentfile.getPath());
				map.put(FileKeysEnum.SIZE.getKey(), currentfile.getSize());
				LOGGER.debug(" current file size : {} ", currentfile.getSize());
				redisManager.insertHASH( // file:SHA1(GUID_pli:fid) => HASH { rel-obj-key: "Façade.jpg", size: "2",
											// mul-id: "..." }
						RedisKeysEnum.FT_FILE.getKey(shaFid), map);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	public static List<PartETag> getPartEtags(RedisManager redisManager, String hashFid) {
		List<PartETag> partETags = new ArrayList<>();
		try {
			Pattern pattern = Pattern.compile(":");
			RedisUtils.getPartEtagsString(redisManager, hashFid).stream().forEach(k -> {
				String[] items = pattern.split(k, 2);
				if (2 == items.length) {
					PartETag partETag = new PartETag(Integer.parseInt(items[0]), items[1]);
					partETags.add(partETag);
				} else {
					throw new RedisTryAgainException("");
				}

			});
			return partETags;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		return partETags;
	}

	public static String addToPartEtags(RedisManager redisManager, PartETag partETag, String hashFid)
			throws UploadException {
		String partEtagRedisForm = "";
		try {
			String key = RedisKeysEnum.FT_PART_ETAGS.getKey(hashFid);
			partEtagRedisForm = partETag.getPartNumber() + ":" + partETag.getETag();
			redisManager.rpush(key, partEtagRedisForm);
			return partEtagRedisForm;
		} catch (Exception e) {
			throw new UploadException("Error adding Etag : " + e.getMessage(), e);
		}
	}

	public static String addToFileMultipartUploadIdContainer(RedisManager redisManager, String uploadId,
			String hashFid) {
		try {
			String key = RedisKeysEnum.FT_ID_CONTAINER.getKey(hashFid);
			redisManager.lpush(key, uploadId);
			return uploadId;
		} catch (Exception e) {
			throw e;
		}
	}

	public static String getUploadIdBlocking(RedisManager redisManager, String hashFid) throws UploadException {
		String keySource = RedisKeysEnum.FT_ID_CONTAINER.getKey(hashFid);
		String uploadOsuId = redisManager.brpoplpush(keySource, keySource, 30);

		if (uploadOsuId == null || uploadOsuId.isBlank() || uploadOsuId.isEmpty()) {
			String uuid = UUID.randomUUID().toString();
			throw new UploadException("Error getting uploadOsuId for hash : " + hashFid, uuid);
		}
		return uploadOsuId;
	}

	private static LocalDateTime getExpiredTimeStamp(int expireDelay) throws UploadException {
		LocalDateTime date = LocalDateTime.now();
		LocalDateTime dateInsert = date.plusDays(expireDelay);
		LocalDateTime maxDate = date.plusDays(maxUpdateDate);
		if (dateInsert.isAfter(maxDate)) {
			throw new UploadException("Date invalide, veuillez sélectionner une date inférieure à " + maxUpdateDate
					+ " jours depuis la création du pli");
		}
		return dateInsert;
	}
}

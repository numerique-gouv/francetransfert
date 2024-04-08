/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.services.sequestre;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.gouv.culture.francetransfert.core.enums.EnclosureKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.exception.StorageException;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.services.StorageManager;
import fr.gouv.culture.francetransfert.core.utils.Base64CryptoService;
import fr.gouv.culture.francetransfert.core.utils.RedisUtils;
import fr.gouv.culture.francetransfert.model.Enclosure;
import fr.gouv.culture.francetransfert.model.Recipient;

@Service
public class SequestreService {
	private static final Logger LOGGER = LoggerFactory.getLogger(SequestreService.class);

	@Value("${bucket.prefix}")
	private String bucketPrefix;

	@Autowired
	StorageManager storageManager;

	@Autowired
	RedisManager redisManager;

	@Autowired
	Base64CryptoService base64CryptoService;

	public void createSequestre(String prefix) throws MetaloadException, StorageException {
		try {
			storageManager.generateBucketSequestre(prefix);
		} catch (Exception e) {
			LOGGER.error("Error in create bucket sequestre : " + e.getMessage(), e);
		}
	}

	public void writeOnSequestre(String enclosureId) {
		try {

			String nameBucketSource = RedisUtils.getBucketName(redisManager, enclosureId, bucketPrefix);
			String fileName = storageManager.getZippedEnclosureName(enclosureId);
			Enclosure enclosure = Enclosure.build(enclosureId, redisManager);
			String sender = RedisUtils.getEmailSenderEnclosure(redisManager, enclosureId);
			String passwordRedis = RedisUtils.getEnclosureValue(redisManager, enclosureId,
					EnclosureKeysEnum.PASSWORD.getKey());
			String passwordUnHashed = base64CryptoService.aesDecrypt(passwordRedis);
			String destinataireList = "";
			List<Recipient> recipients = enclosure.getRecipients();
			if (CollectionUtils.isNotEmpty(recipients)) {
				destinataireList = "{" + recipients.stream().map(x -> {
					return x.getMail();
				}).collect(Collectors.joining(" , ")) + "}";
			}

			// Log Sequestre
			LOGGER.warn(
					"[SEQUESTRE] - enclosure: {} , sender: {} , password: {} , fileName: {} , recipients: {} - [SEQUESTRE]",
					enclosureId, sender, passwordUnHashed, fileName, destinataireList);

			storageManager.moveOnSequestre(nameBucketSource, fileName);
		} catch (Exception e) {
			LOGGER.error("Error while coping enclosudre {} in sequestre : {}", enclosureId, e.getMessage(), e);
		}
	}

	public void removeEnclosureSequestre(String enclosureId) {
		List<RedisKeysEnum> RedisKeysEnumvalidate = new ArrayList<>();
		for (RedisKeysEnum val : RedisKeysEnum.values()) {
			if (val.getFirstKeyPart().startsWith("enclosure:")) {
				RedisKeysEnumvalidate.add(val);
			}
		}
		try {
			RedisKeysEnumvalidate.forEach(val -> {
				redisManager.renameKey(val.getKey(enclosureId),
						val.getKey(enclosureId).replace("enclosure:", "sequestre:"));
			});
		} catch (Exception e) {
			LOGGER.error("Error find key while moving to sequestre : " + e.getMessage(), e);
		}
	}
}

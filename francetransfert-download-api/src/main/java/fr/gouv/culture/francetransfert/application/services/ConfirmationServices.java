/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.services;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.utils.RedisUtils;
import fr.gouv.culture.francetransfert.domain.exceptions.DownloadException;

@Service
public class ConfirmationServices {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmationServices.class);

	@Autowired
	private RedisManager redisManager;

	public void validateToken(String senderMail, String token) throws MetaloadException {
		// verify token in redis
		LOGGER.debug("check token for sender mail {}", senderMail);
		redisManager.validateToken(senderMail, token);
	}

	public boolean isReceiver(String plis, String mailAdress) {
		return redisManager.sexists(RedisKeysEnum.FT_RECEIVE.getKey(mailAdress.toLowerCase()), plis);
	}

	public void validateRecipientId(String enclosureId, String recipientMail, String recipientId) {
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

}

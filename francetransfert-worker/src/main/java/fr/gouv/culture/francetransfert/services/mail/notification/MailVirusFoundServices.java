/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.services.mail.notification;

import java.util.Locale;

import org.apache.commons.lang3.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.gouv.culture.francetransfert.core.enums.EnclosureKeysEnum;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.utils.RedisUtils;
import fr.gouv.culture.francetransfert.model.Enclosure;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MailVirusFoundServices {

	private static final Logger LOGGER = LoggerFactory.getLogger(MailVirusFoundServices.class);

	@Autowired
	MailNotificationServices mailNotificationServices;

	@Autowired
	RedisManager redisManager;

	// Send mail to sender
	public void sendToSender(Enclosure enclosure, String templateName, String subject, Locale currentLanguage)
			throws MetaloadException {
		LOGGER.info("send email notification virus to sender: {}", enclosure.getSender());

		Locale language = Locale.FRANCE;
		try {
			language = LocaleUtils.toLocale(RedisUtils.getEnclosureValue(redisManager, enclosure.getGuid(),
					EnclosureKeysEnum.LANGUAGE.getKey()));
		} catch (Exception eL) {
			LOGGER.error("Error while getting local", eL);
		}

		mailNotificationServices.prepareAndSend(enclosure.getSender(), subject, enclosure, templateName, language);
	}
}

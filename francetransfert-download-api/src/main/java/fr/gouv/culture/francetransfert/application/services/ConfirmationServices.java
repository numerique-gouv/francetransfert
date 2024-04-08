/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.services.RedisManager;

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

}

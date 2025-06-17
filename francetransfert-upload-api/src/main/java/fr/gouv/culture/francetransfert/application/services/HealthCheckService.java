/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.model.HealthCheckRepresentation;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.services.StorageManager;

@Service
public class HealthCheckService {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private RedisManager redisManager;

	@Autowired
	private StorageManager storageManager;

	@Value("${healthcheck.config.url:''}")
	private String configUrl;

	@Value("${healthcheck.smtp.host:''}")
	private String smtpHost;

	@Value("${healthcheck.smtp.port:0}")
	private int smtpPort;

	@Value("${healthcheck.smtp.delay:300000}")
	private int smtpAllowedDelay;

	private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckService.class);

	public HealthCheckRepresentation healthCheck() {

		String jsonInString = redisManager.getString(RedisKeysEnum.HEALTHCHECK.getFirstKeyPart());

		HealthCheckRepresentation health = new Gson().fromJson(jsonInString, HealthCheckRepresentation.class);

		return health;

	}

}

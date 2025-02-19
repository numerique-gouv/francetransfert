/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.healthcheck.services;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

import fr.gouv.culture.francetransfert.core.enums.GlimpsHealthCheckEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisQueueEnum;
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

	public void healthCheck() {

		boolean redis = false;
		boolean s3 = false;
		boolean config = false;
		boolean smtp = false;
		boolean smtpDelayOk = true;
		boolean glimps = false;
		long zipQueueSize = 0;
		// int smtpDelay = -1;
		// int smtpPending = -1;

		try {
			redis = redisManager.ping().equalsIgnoreCase("PONG");
		} catch (Exception e) {
			LOGGER.error("Error while checking redis", e);
		}

		try {
			s3 = storageManager.healthCheckQuery();
		} catch (Exception e) {
			LOGGER.error("Error while checking s3", e);
		}

		try {
			config = restTemplate.exchange(configUrl, HttpMethod.GET, null, String.class).getBody()
					.contains("mimeType");
		} catch (Exception e) {
			LOGGER.error("Error while checking config", e);
		}

		try {
			smtp = checkMail();
		} catch (Exception e) {
			LOGGER.error("Error while checking smtp", e);
		}

		try {
			zipQueueSize = redisManager.llen(RedisQueueEnum.ZIP_QUEUE.getValue());
		} catch (Exception e) {
			LOGGER.error("Error while checking zip queue size", e);
		}

		// try {
		// Map<String, String> hashRedis =
		// redisManager.hmgetAllString(RedisKeysEnum.CHECK_MAIL.getFirstKeyPart());
		// smtpUuid = hashRedis.getOrDefault(CheckMailKeysEnum.UUID.getKey(), "");
		// smtpPending =
		// Integer.parseInt(hashRedis.getOrDefault(CheckMailKeysEnum.PENDING.getKey(),
		// "-1"));
		// smtpDelay =
		// Integer.parseInt(hashRedis.getOrDefault(CheckMailKeysEnum.DELAY.getKey(),
		// "-1"));
		// if (smtpDelay < smtpAllowedDelay) {
		// smtpDelayOk = true;
		// }
		// } catch (Exception e) {
		// LOGGER.error("Error while checking smtp delay", e);
		// }

		try {
			glimps = Boolean.parseBoolean(redisManager.getString(GlimpsHealthCheckEnum.STATE.getKey()));
		} catch (Exception e) {
			LOGGER.error("Error while checking glimps", e);
		}

		HealthCheckRepresentation health = HealthCheckRepresentation.builder().smtp(smtp).s3(s3).redis(redis)
				.config(config).smtpDelayOk(smtpDelayOk).glimps(glimps).zipQueueSize(zipQueueSize).build();

		String jsonInString = new Gson().toJson(health);
		redisManager.setString(RedisKeysEnum.HEALTHCHECK.getFirstKeyPart(), jsonInString);

	}

	private boolean checkMail() throws Exception {
		try (Socket s = new Socket()) {
			s.setReuseAddress(true);
			SocketAddress sa = new InetSocketAddress(smtpHost, smtpPort);
			s.connect(sa, 5000);
			s.close();
			return true;
		}
	}

}

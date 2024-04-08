/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fr.gouv.culture.francetransfert.core.enums.RedisQueueEnum;
import fr.gouv.culture.francetransfert.core.enums.TypeStat;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.services.stat.StatServices;

@Component
public class StatTask implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(StatTask.class);

	private StatServices statServices;

	private RedisManager redisManager;

	private String statMessage;

	public StatTask(String statMessage, RedisManager redisManager, StatServices statServices) {
		this.statMessage = statMessage;
		this.redisManager = redisManager;
		this.statServices = statServices;
	}

	public StatTask() {

	}

	@Override
	public void run() {
		LOGGER.info("[Worker] : start stat process for message  {}", statMessage);
		try {
			LOGGER.info("ThreadName: " + Thread.currentThread().getName() + " | ThreadId: "
					+ Thread.currentThread().getId());
			String[] splittedMessage = statMessage.split(";");
			TypeStat type = TypeStat.valueOf(splittedMessage[0]);
			String enclosureId = splittedMessage[1];
			String recipient = "";
			if (splittedMessage.length > 2) {
				recipient = splittedMessage[2];
			}
			LOGGER.info("start save data in csv : " + enclosureId + "and type " + type.name());
			if (TypeStat.UPLOAD.equals(type)) {
				statServices.saveDataUpload(enclosureId);
				redisManager.publishFT(RedisQueueEnum.TEMP_DATA_CLEANUP_QUEUE.getValue(), enclosureId);
			} else if (TypeStat.DOWNLOAD.equals(type)) {
				statServices.saveDataDownload(enclosureId, recipient);
			}
		} catch (Exception e) {
			LOGGER.error("[Worker] stat error : " + e.getMessage(), e);
		}
	}
}

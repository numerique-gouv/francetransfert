/*
 *   * Copyright (c) Ministère de la Culture (2022) 
 * 
 * SPDX-License-Identifier: Apache-2.0 
 * License-Filename: LICENSE.txt 
 */


 
package fr.gouv.culture.francetransfert.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fr.gouv.culture.francetransfert.services.cleanup.CleanUpServices;

@Component
public class TempDataCleanupTask implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(TempDataCleanupTask.class);

	private CleanUpServices cleanUpServices;

	private String enclosureId;

	public String getEnclosureId() {
		return enclosureId;
	}

	public TempDataCleanupTask(String enclosureId, CleanUpServices cleanUpServices) {
		this.enclosureId = enclosureId;
		this.cleanUpServices = cleanUpServices;
	}

	public TempDataCleanupTask() {

	}

	@Override
	public void run() {
		try {
			LOGGER.info("ThreadName: " + Thread.currentThread().getName() + " | ThreadId: "
					+ Thread.currentThread().getId());
			LOGGER.info(" start temp data cleanup process for enclosure N: {}", enclosureId);
			cleanUpServices.cleanUpEnclosureTempDataInRedis(enclosureId, true);
		} catch (Exception e) {
			LOGGER.error("[Worker] temp data cleanup error : " + e.getMessage(), e);
		}
		LOGGER.info("ThreadName: " + Thread.currentThread().getName() + " | ThreadId: " + Thread.currentThread().getId()
				+ " IS DEAD");
	}
}

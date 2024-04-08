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

import fr.gouv.culture.francetransfert.services.cleanup.CleanUpServices;

@Component
public class CleanEnclosureTask implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(CleanEnclosureTask.class);

	private String enclosureId;

	public String getEnclosureId() {
		return enclosureId;
	}

	private CleanUpServices cleanService;

	public CleanEnclosureTask(String enclosureId, CleanUpServices cleanService) {
		this.enclosureId = enclosureId;
		this.cleanService = cleanService;
	}

	public CleanEnclosureTask() {

	}

	@Override
	public void run() {
		try {
			LOGGER.info("[Worker] Clean EnclosureId from Delete");
			LOGGER.info("ThreadName: " + Thread.currentThread().getName() + " | ThreadId: "
					+ Thread.currentThread().getId());
			cleanService.cleanEnclosure(enclosureId, true);

		} catch (Exception e) {
			LOGGER.error("[Worker] Clean EnclosureId error : " + e.getMessage(), e);
		}
	}
}

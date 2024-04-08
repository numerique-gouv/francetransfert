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

import fr.gouv.culture.francetransfert.services.zipworker.ZipWorkerServices;

@Component
public class ZipWorkerTask implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(ZipWorkerTask.class);

	private ZipWorkerServices zipWorkerServices;

	private String enclosureId;

	public String getEnclosureId() {
		return enclosureId;
	}

	public ZipWorkerTask(String enclosureId, ZipWorkerServices zipWorkerServices) {
		this.enclosureId = enclosureId;
		this.zipWorkerServices = zipWorkerServices;
	}

	public ZipWorkerTask() {

	}

	@Override
	public void run() {
		LOGGER.debug("[Worker] Start zip  process for enclosur N°  {}", enclosureId);
		try {
			LOGGER.debug("ThreadName: " + Thread.currentThread().getName() + " | ThreadId: "
					+ Thread.currentThread().getId());
			zipWorkerServices.startZip(enclosureId);
		} catch (Exception e) {
			LOGGER.error("[Worker] Zip worker error : " + e.getMessage(), e);
		}
		LOGGER.debug("ThreadName: " + Thread.currentThread().getName() + " | ThreadId: "
				+ Thread.currentThread().getId() + " IS DEAD");
	}
}

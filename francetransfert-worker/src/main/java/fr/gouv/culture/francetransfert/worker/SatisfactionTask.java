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

import fr.gouv.culture.francetransfert.core.model.RateRepresentation;
import fr.gouv.culture.francetransfert.services.satisfaction.SatisfactionService;

@Component
public class SatisfactionTask implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(SatisfactionTask.class);

	private SatisfactionService satisfactionService;

	private RateRepresentation rate;

	public SatisfactionTask(RateRepresentation rate, SatisfactionService satisfactionService) {
		this.rate = rate;
		this.satisfactionService = satisfactionService;
	}

	public SatisfactionTask() {

	}

	@Override
	public void run() {
		try {
			LOGGER.info("[Worker] Save satisfaction data");
			LOGGER.info("ThreadName: " + Thread.currentThread().getName() + " | ThreadId: "
					+ Thread.currentThread().getId());
			satisfactionService.saveData(rate);

		} catch (Exception e) {
			LOGGER.error("[Worker] satisfaction error : " + e.getMessage(), e);
		}
	}
}

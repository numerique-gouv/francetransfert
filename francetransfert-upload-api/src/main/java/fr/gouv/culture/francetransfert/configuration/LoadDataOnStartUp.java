/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LoadDataOnStartUp {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoadDataOnStartUp.class);

	private static final String DEV = "DEV";
	private static final String CONFIG_PATH_KEY = "APP_CONF_PATH";

	@Value("${environnement}")
	private String environnement;

	@EventListener(ApplicationStartedEvent.class)
	public void appReady() {
		LOGGER.info("ApplicationStartedEvent : " + environnement);
		if (!DEV.equalsIgnoreCase(environnement) && StringUtils.isNotBlank(System.getenv(CONFIG_PATH_KEY))) {
			Path confPath = Paths.get(System.getenv(CONFIG_PATH_KEY));
			try {
				Files.deleteIfExists(confPath);
				LOGGER.info("Deleting configuration file : " + System.getenv(CONFIG_PATH_KEY));
			} catch (IOException e) {
				LOGGER.error("Cannot delete configuration file : " + System.getenv(CONFIG_PATH_KEY));
			}
		}
	}
}

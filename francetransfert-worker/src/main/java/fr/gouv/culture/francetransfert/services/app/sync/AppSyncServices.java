/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.services.app.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.gouv.culture.francetransfert.core.enums.AppSyncKeysEnum;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.security.WorkerException;

@Service
public class AppSyncServices {

	private static final Logger LOGGER = LoggerFactory.getLogger(AppSyncServices.class);

	@Autowired
	RedisManager redisManager;

	public void appSyncCleanup() {
		try {
			redisManager.deleteKey(AppSyncKeysEnum.APP_SYNC_CLEANUP.getKey());
		} catch (Exception e) {
			throw new WorkerException(e.getMessage());
		}
	}

	public void appSyncRelaunch() {
		try {
			redisManager.deleteKey(AppSyncKeysEnum.APP_SYNC_RELAUNCH.getKey());
		} catch (Exception e) {
			throw new WorkerException(e.getMessage());
		}
	}

	public void appSyncRefDomain() {
		try {
			redisManager.deleteKey(AppSyncKeysEnum.APP_SYNC_IGNIMISSION_DOMAIN.getKey());
		} catch (Exception e) {
			throw new WorkerException(e.getMessage());
		}
	}

	public void appSyncCheckMailCheck() {
		try {
			redisManager.deleteKey(AppSyncKeysEnum.APP_SYNC_CHECK_MAIL_CHECK.getKey());
		} catch (Exception e) {
			throw new WorkerException(e.getMessage());
		}
	}

	public void appSyncGlimpsCheck() {
		try {
			redisManager.deleteKey(AppSyncKeysEnum.APP_SYNC_CHECK_GLIMPS.getKey());
		} catch (Exception e) {
			throw new WorkerException(e.getMessage());
		}
	}

	public void appSyncCheckMailSend() {
		try {
			redisManager.deleteKey(AppSyncKeysEnum.APP_SYNC_CHECK_MAIL_SEND.getKey());
		} catch (Exception e) {
			throw new WorkerException(e.getMessage());
		}
	}

	public boolean shouldRelaunch() {
		boolean shouldRelaunch = false;
		try {
			if (redisManager.incr(AppSyncKeysEnum.APP_SYNC_RELAUNCH.getKey()) == 1) {
				LOGGER.info("Check Ok du Lancement du batch des mails de relance");
				shouldRelaunch = true;
			}
		} catch (Exception e) {
			throw new WorkerException(e.getMessage());
		}
		return shouldRelaunch;
	}

	public boolean shouldCleanup() {
		boolean shouldCleanup = false;
		try {
			if (redisManager.incr(AppSyncKeysEnum.APP_SYNC_CLEANUP.getKey()) == 1) {
				LOGGER.info("Check Ok du Lancement du batch de nettoyage");
				shouldCleanup = true;
			}
		} catch (Exception e) {
			throw new WorkerException(e.getMessage());
		}
		return shouldCleanup;
	}

	public boolean shouldUpdateRefDomain() {
		boolean shouldUpdateDomain = false;
		try {
			if (redisManager.incr(AppSyncKeysEnum.APP_SYNC_IGNIMISSION_DOMAIN.getKey()) == 1) {
				LOGGER.info("Check Ok du Lancement du batch synchro des domaines");
				shouldUpdateDomain = true;
			}
		} catch (Exception e) {
			throw new WorkerException(e.getMessage());
		}
		return shouldUpdateDomain;
	}

	public boolean shouldCheckMailCheck() {
		boolean shouldCheckMail = false;
		try {
			if (redisManager.incr(AppSyncKeysEnum.APP_SYNC_CHECK_MAIL_CHECK.getKey()) == 1) {
				LOGGER.info("Check Ok du Lancement du check des mails");
				shouldCheckMail = true;
			}
		} catch (Exception e) {
			throw new WorkerException(e.getMessage());
		}
		return shouldCheckMail;
	}

	public boolean shouldSendCheckMail() {
		boolean shouldCheckMail = false;
		try {
			if (redisManager.incr(AppSyncKeysEnum.APP_SYNC_CHECK_MAIL_SEND.getKey()) == 1) {
				LOGGER.info("Check Ok du Lancement de l'envoi des mails");
				shouldCheckMail = true;
			}
		} catch (Exception e) {
			throw new WorkerException(e.getMessage());
		}
		return shouldCheckMail;
	}

	public boolean shouldCheckGlimps() {
		boolean shouldCheckGlimps = false;
		try {
			if (redisManager.incr(AppSyncKeysEnum.APP_SYNC_CHECK_GLIMPS.getKey()) == 1) {
				LOGGER.info("Check Ok du check de glimps");
				shouldCheckGlimps = true;
			}
		} catch (Exception e) {
			throw new WorkerException(e.getMessage());
		}
		return shouldCheckGlimps;
	}

}

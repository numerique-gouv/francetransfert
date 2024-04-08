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

import fr.gouv.culture.francetransfert.services.mail.notification.MailConfirmationCodeServices;

@Component
public class SendEmailConfirmationCodeTask implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(SendEmailConfirmationCodeTask.class);

	private MailConfirmationCodeServices mailConfirmationCodeServices;

	private String mailCode;

	public String getMailCode() {
		return mailCode;
	}

	public SendEmailConfirmationCodeTask(String mailCode, MailConfirmationCodeServices mailConfirmationCodeServices) {
		this.mailCode = mailCode;
		this.mailConfirmationCodeServices = mailConfirmationCodeServices;
	}

	public SendEmailConfirmationCodeTask() {

	}

	@Override
	public void run() {
		try {
			LOGGER.info("[Worker] Start send confirmation code : " + mailCode);
			mailConfirmationCodeServices.sendConfirmationCode(mailCode);
		} catch (Exception e) {
			LOGGER.error("[Worker] Send mail confirmation code error : " + e.getMessage(), e);
		}
	}
}

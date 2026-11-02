/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

package fr.gouv.culture.francetransfert.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fr.gouv.culture.francetransfert.core.model.ConfirmationCodeMessage;
import fr.gouv.culture.francetransfert.services.mail.notification.MailConfirmationCodeServices;

@Component
public class SendEmailConfirmationCodeTask implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(SendEmailConfirmationCodeTask.class);

	private MailConfirmationCodeServices mailConfirmationCodeServices;

	private ConfirmationCodeMessage confirmationCodeMessage;

	public SendEmailConfirmationCodeTask(ConfirmationCodeMessage confirmationCodeMessage,
			MailConfirmationCodeServices mailConfirmationCodeServices) {
		this.confirmationCodeMessage = confirmationCodeMessage;
		this.mailConfirmationCodeServices = mailConfirmationCodeServices;
	}

	public SendEmailConfirmationCodeTask() {

	}

	@Override
	public void run() {
		try {
			LOGGER.debug("[Worker] Start send confirmation code to : {}",
					confirmationCodeMessage != null ? confirmationCodeMessage.getSenderMail() : null);
			mailConfirmationCodeServices.sendConfirmationCode(confirmationCodeMessage);
		} catch (Exception e) {
			LOGGER.error("[Worker] Send mail confirmation code error : " + e.getMessage(), e);
		}
	}
}

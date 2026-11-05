/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.services.mail.notification;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.gouv.culture.francetransfert.core.model.ConfirmationCodeMessage;
import fr.gouv.culture.francetransfert.model.ConfirmationCode;
import fr.gouv.culture.francetransfert.security.WorkerException;
import fr.gouv.culture.francetransfert.services.mail.notification.enums.NotificationTemplateEnum;
import fr.gouv.culture.francetransfert.core.utils.LanguageUtils;

@Service
public class MailConfirmationCodeServices {

	private static final Logger LOGGER = LoggerFactory.getLogger(MailConfirmationCodeServices.class);

	private static final DateTimeFormatter HEURE_EXPIRATION = DateTimeFormatter.ofPattern("HH:mm:ss");

	@Autowired
	private MailNotificationServices mailNotificationServices;

	@Value("${subject.confirmation.code}")
	private String subjectConfirmationCode;

	@Value("${subject.confirmation.codeEn}")
	private String subjectConfirmationCodeEn;

	@Value("${expire.confirmation.code}")
	private int secondsToExpireConfirmationCode;

	@Value("${expire.token.sender}")
	private int expireTokenSender;

	public void sendConfirmationCode(ConfirmationCodeMessage message) {
		LOGGER.debug("STEP SEND MAIL ");
		if (message == null || StringUtils.isBlank(message.getSenderMail()) || StringUtils.isBlank(message.getCode())) {
			LOGGER.error("Invalid confirmation code message");
			throw new WorkerException("invalid confirmation code message");
		}
		String senderMail = message.getSenderMail();
		String code = message.getCode();
		String ttlCode = extractHeureExpirationCode(message.getExpiration());
		int codeMin = secondsToExpireConfirmationCode / 60;
		int sessionMin = expireTokenSender / 60;
		Locale currentLanguage = LanguageUtils.toSupportedLocale(message.getLanguage());

		ConfirmationCode confirmationCode = ConfirmationCode.builder().code(code).mail(senderMail)
				.dateExpiration(ttlCode).codeTime(codeMin).sessionTime(sessionMin).build();
		LOGGER.info("Send email confirmation code to sender:  {}", senderMail);
		if (Locale.UK.equals(currentLanguage)) {
			mailNotificationServices.prepareAndSend(senderMail, subjectConfirmationCodeEn, confirmationCode,
					NotificationTemplateEnum.MAIL_CONFIRMATION_CODE.getValue(), currentLanguage);
		} else {
			mailNotificationServices.prepareAndSend(senderMail, subjectConfirmationCode, confirmationCode,
					NotificationTemplateEnum.MAIL_CONFIRMATION_CODE.getValue(), currentLanguage);
		}

	}

	private String extractHeureExpirationCode(String expiration) {
		if (StringUtils.isBlank(expiration)) {
			return "";
		}
		try {
			return ZonedDateTime.parse(expiration).format(HEURE_EXPIRATION);
		} catch (DateTimeParseException e) {
			LOGGER.warn("Unable to parse confirmation code expiration {}", expiration);
			return "";
		}
	}

}

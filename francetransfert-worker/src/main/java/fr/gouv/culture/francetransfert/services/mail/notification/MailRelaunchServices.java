/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.services.mail.notification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import fr.gouv.culture.francetransfert.core.enums.EnclosureKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RecipientKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.StatutEnum;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.utils.DateUtils;
import fr.gouv.culture.francetransfert.core.utils.RedisUtils;
import fr.gouv.culture.francetransfert.model.Enclosure;
import fr.gouv.culture.francetransfert.model.Recipient;
import fr.gouv.culture.francetransfert.security.WorkerException;
import fr.gouv.culture.francetransfert.services.mail.notification.enums.NotificationTemplateEnum;

@Service
public class MailRelaunchServices {

	private static final Logger LOGGER = LoggerFactory.getLogger(MailRelaunchServices.class);

	@Value("${relaunch.mail.days}")
	private int relaunchDays;

	@Value("${subject.relaunch.recipient}")
	private String subjectRelaunchRecipient;

	@Value("${subject.relaunch.recipientEn}")
	private String subjectRelaunchRecipientEn;

	@Autowired
	MailNotificationServices mailNotificationServices;

	@Autowired
	RedisManager redisManager;

	private static List<String> ALLOWED_CODE = Arrays.asList(StatutEnum.PAT.getCode(), StatutEnum.ECT.getCode());

	public void sendMailsRelaunch() throws WorkerException {
		redisManager.smembersString(RedisKeysEnum.FT_ENCLOSURE_DATES.getKey("")).forEach(date -> {
			redisManager.smembersString(RedisKeysEnum.FT_ENCLOSURE_DATE.getKey(date)).forEach(enclosureId -> {
				try {
					LocalDateTime exipireEnclosureDate = DateUtils.convertStringToLocalDateTime(
							redisManager.getHgetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
									EnclosureKeysEnum.EXPIRED_TIMESTAMP.getKey()));
					Map<String, String> enclosureRedis = RedisUtils.getEnclosure(redisManager, enclosureId);
					String statusCode = enclosureRedis.get(EnclosureKeysEnum.STATUS_CODE.getKey());
					if (!ALLOWED_CODE.contains(statusCode)) {
						return;
					}
					if (LocalDate.now().equals(exipireEnclosureDate.toLocalDate().minusDays(relaunchDays))) {
						Enclosure enclosure = Enclosure.build(enclosureId, redisManager);
						LOGGER.info(" send relaunch mail for enclosure N° {}", enclosureId);
						sendToRecipientsAndSenderRelaunch(enclosure,
								NotificationTemplateEnum.MAIL_RELAUNCH_RECIPIENT.getValue());
					}
				} catch (Exception e) {
					LOGGER.error("Cannot send relaunch for enclosure {} :  {}", enclosureId, e.getMessage(), e);
				}
			});
		});
	}

	// Send mails Relaunch to recipients
	private void sendToRecipientsAndSenderRelaunch(Enclosure enclosure, String templateName)
			throws WorkerException, MetaloadException {
		List<Recipient> recipients = enclosure.getRecipients();
		Locale language = Locale.FRANCE;
		try {
			language = LocaleUtils.toLocale(RedisUtils.getEnclosureValue(redisManager, enclosure.getGuid(),
					EnclosureKeysEnum.LANGUAGE.getKey()));
		} catch (Exception eL) {
			LOGGER.error("Error while getting local", eL);
		}
		String sendRelaunchRecipient = new String(subjectRelaunchRecipient);
		if (Locale.UK.equals(language)) {
			sendRelaunchRecipient = new String(subjectRelaunchRecipientEn);
		}

		if (!CollectionUtils.isEmpty(recipients)) {
			if (StringUtils.isNotBlank(enclosure.getSubject())) {
				sendRelaunchRecipient = sendRelaunchRecipient.concat(" : ").concat(enclosure.getSubject());
			}
			for (Recipient recipient : recipients) {
				Map<String, String> recipientMap = RedisUtils.getRecipientEnclosure(redisManager, recipient.getId());
				boolean isFileDownloaded = (!CollectionUtils.isEmpty(recipientMap)
						&& 0 == Integer.parseInt(recipientMap.get(RecipientKeysEnum.NB_DL.getKey())));
				if (isFileDownloaded) {
					enclosure.setUrlDownload(mailNotificationServices.generateUrlForDownload(enclosure.getGuid(),
							recipient.getMail(), recipient.getId()));
					LOGGER.info(" send relaunch mail to {} ", recipient.getMail());

					mailNotificationServices.prepareAndSend(recipient.getMail(), sendRelaunchRecipient, enclosure,
							templateName, language);
				}
			}
		}
	}
}

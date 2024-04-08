/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.services.mail.notification;

import java.util.ArrayList;
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
import fr.gouv.culture.francetransfert.core.enums.StatutEnum;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.utils.RedisUtils;
import fr.gouv.culture.francetransfert.model.Enclosure;
import fr.gouv.culture.francetransfert.model.Recipient;
import fr.gouv.culture.francetransfert.services.mail.notification.enums.NotificationTemplateEnum;

@Service
public class MailEnclosureNoLongerAvailbleServices {

	private static final Logger LOGGER = LoggerFactory.getLogger(MailEnclosureNoLongerAvailbleServices.class);

	@Value("${subject.no.availble.enclosure.recipient}")
	private String subjectNoAvailbleEnclosureRecipient;

	@Value("${subject.no.availble.enclosure.sender}")
	private String subjectNoAvailbleEnclosureSender;

	@Value("${subject.no.availble.enclosure.recipientEn}")
	private String subjectNoAvailbleEnclosureRecipientEn;

	@Value("${subject.no.availble.enclosure.senderEn}")
	private String subjectNoAvailbleEnclosureSenderEn;

	@Autowired
	private MailNotificationServices mailNotificationServices;

	@Autowired
	RedisManager redisManager;

	private static List<String> ALLOWED_CODE = Arrays.asList(StatutEnum.PAT.getCode(), StatutEnum.ECT.getCode());

	public void sendEnclosureNotAvailble(Enclosure enclosure) throws MetaloadException {

		if (!ALLOWED_CODE.contains(enclosure.getStatut())) {
			return;
		}

		List<Recipient> recipients = enclosure.getRecipients();
		String sendNoAvailbleEnclosureRecipient = new String(subjectNoAvailbleEnclosureRecipient);
		String sendNoAvailbleEnclosureSender = new String(subjectNoAvailbleEnclosureSender);

		Locale language = Locale.FRANCE;
		try {
			language = LocaleUtils.toLocale(RedisUtils.getEnclosureValue(redisManager, enclosure.getGuid(),
					EnclosureKeysEnum.LANGUAGE.getKey()));
		} catch (Exception eL) {
			LOGGER.error("Error while getting local", eL);
		}

		if (Locale.UK.equals(language)) {
			sendNoAvailbleEnclosureRecipient = new String(subjectNoAvailbleEnclosureRecipientEn);
			sendNoAvailbleEnclosureSender = new String(subjectNoAvailbleEnclosureSenderEn);
		}

		if (!CollectionUtils.isEmpty(recipients)) {
			if (StringUtils.isNotBlank(enclosure.getSubject())) {
				sendNoAvailbleEnclosureRecipient = sendNoAvailbleEnclosureRecipient.concat(" : ")
						.concat(enclosure.getSubject());
				sendNoAvailbleEnclosureSender = sendNoAvailbleEnclosureSender.concat(" : ")
						.concat(enclosure.getSubject());
			}
			List<Recipient> recipientsDoNotDownloadedEnclosure = new ArrayList<>();
			for (Recipient recipient : recipients) {
				Map<String, String> recipientMap = RedisUtils.getRecipientEnclosure(redisManager, recipient.getId());
				boolean isFileDownloaded = (!CollectionUtils.isEmpty(recipientMap)
						&& 0 == Integer.parseInt(recipientMap.get(RecipientKeysEnum.NB_DL.getKey())));
				if (isFileDownloaded) {
					recipientsDoNotDownloadedEnclosure.add(recipient);
					mailNotificationServices.prepareAndSend(recipient.getMail(), sendNoAvailbleEnclosureRecipient,
							enclosure, NotificationTemplateEnum.MAIL_ENCLOSURE_NO_AVAILBLE_RECIPIENTS.getValue(),
							language);
					LOGGER.info("send email notification enclosure not availble to recipient: {}", recipient.getMail());
				}
			}
			// Send email to the sender of enclosure is no longer available for download to
			// recipients who have not removed it in time
			if (!CollectionUtils.isEmpty(recipientsDoNotDownloadedEnclosure)) {
				enclosure.setNotDownloadRecipients(recipientsDoNotDownloadedEnclosure);
				mailNotificationServices.prepareAndSend(enclosure.getSender(), sendNoAvailbleEnclosureSender, enclosure,
						NotificationTemplateEnum.MAIL_ENCLOSURE_NO_AVAILBLE_SENDER.getValue(), language);
				LOGGER.info("send email notification enclosure not availble to sender: {}", enclosure.getSender());
			}
		}
	}
}

/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.services.mail.notification;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import com.google.gson.JsonObject;

import fr.gouv.culture.francetransfert.core.enums.EnclosureKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.SourceEnum;
import fr.gouv.culture.francetransfert.core.enums.StatutEnum;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.exception.StatException;
import fr.gouv.culture.francetransfert.core.model.NewRecipient;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.utils.Base64CryptoService;
import fr.gouv.culture.francetransfert.core.utils.RedisUtils;
import fr.gouv.culture.francetransfert.core.utils.StringUploadUtils;
import fr.gouv.culture.francetransfert.model.Enclosure;
import fr.gouv.culture.francetransfert.model.NotificationContent;
import fr.gouv.culture.francetransfert.model.Recipient;
import fr.gouv.culture.francetransfert.services.mail.notification.enums.NotificationTemplateEnum;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MailAvailbleEnclosureServices {

	private static final Logger LOGGER = LoggerFactory.getLogger(MailAvailbleEnclosureServices.class);

	@Autowired
	private MailNotificationServices mailNotificationServices;

	@Autowired
	private RedisManager redisManager;

	@Value("${subject.sender}")
	private String subjectSender;

	@Value("${subject.sender.link}")
	private String subjectSenderLink;

	@Value("${subject.recipient}")
	private String subjectRecipient;

	@Value("${subject.sender.password}")
	private String subjectSenderPassword;

	@Value("${subject.recipient.password}")
	private String subjectRecipientPassword;

	@Value("${subject.senderEn}")
	private String subjectSenderEn;

	@Value("${subject.sender.linkEn}")
	private String subjectSenderLinkEn;

	@Value("${subject.recipientEn}")
	private String subjectRecipientEn;

	@Value("${subject.sender.passwordEn}")
	private String subjectSenderPasswordEn;

	@Value("${subject.recipient.passwordEn}")
	private String subjectRecipientPasswordEn;

	@Value("${rizomo.notif.url:}")
	private String rizomoNotifUrl;

	@Value("${rizomo.auth.token:test}")
	private String rizomoToken;

	@Value("${rizomo.auth.header:X-Api-Key}")
	private String rizomoHeaderToken;

	@Value("${resana.mail.url:test}")
	private String urlPatternResana;

	@Value("${osmose.mail.url:test}")
	private String urlPatternOsmose;

	@Autowired
	Base64CryptoService base64CryptoService;

	@Autowired
	private StringUploadUtils stringUploadUtils;

	@Autowired
	private RestTemplate restTemplate;

	// Send Mails to snder and recipients
	public void sendMailsAvailableEnclosure(Enclosure enclosure, NewRecipient metaDataRecipient, Locale currentLanguage)
			throws MetaloadException, StatException {

		redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
				EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.EDC.getCode(), -1);
		redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
				EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.EDC.getWord(), -1);

		LOGGER.info("send email notification availble to sender: {} for enclosure {}", enclosure.getSender(),
				enclosure.getGuid());
		String passwordRedis = RedisUtils.getEnclosureValue(redisManager, enclosure.getGuid(),
				EnclosureKeysEnum.PASSWORD.getKey());
		boolean publicLink = mailNotificationServices.getPublicLink(enclosure.getGuid());
		String passwordUnHashed = base64CryptoService.aesDecrypt(passwordRedis);
		enclosure.setPassword(passwordUnHashed);
		passwordUnHashed = "";
		passwordRedis = "";
		enclosure.setPublicLink(publicLink);
		enclosure.setUrlAdmin(mailNotificationServices.generateUrlAdmin(enclosure.getGuid()));

		Locale language = Locale.FRANCE;
		try {
			language = LocaleUtils.toLocale(RedisUtils.getEnclosureValue(redisManager, enclosure.getGuid(),
					EnclosureKeysEnum.LANGUAGE.getKey()));
		} catch (Exception eL) {
			LOGGER.error("Error while getting local", eL);
		}

		String subjectSend = new String();
		String subjectSenderPassw = new String();
		String subjectRecipientLang = new String();
		String subjectSenderLinkLang = new String();

		if (Locale.UK.equals(language)) {
			subjectSend = new String(subjectSenderEn);
			subjectSenderPassw = new String(subjectSenderPasswordEn);
			subjectRecipientLang = new String(subjectRecipientEn);
			subjectSenderLinkLang = new String(subjectSenderLinkEn);
		} else {
			subjectSend = new String(subjectSender);
			subjectSenderPassw = new String(subjectSenderPassword);
			subjectRecipientLang = new String(subjectRecipient);
			subjectSenderLinkLang = new String(subjectSenderLink);
		}

		if (publicLink) {
			enclosure.setUrlDownload(mailNotificationServices.generateUrlPublicForDownload(enclosure.getGuid()));
			subjectSend = subjectSenderLinkLang;
		}
		if (StringUtils.isNotBlank(enclosure.getSubject())) {
			subjectSend = subjectSend.concat(enclosure.getSubject());
			subjectSenderPassw = subjectSenderPassw.concat(enclosure.getSubject());
		}
		if (metaDataRecipient == null) {
			mailNotificationServices.prepareAndSend(enclosure.getSender(), subjectSend, enclosure,
					NotificationTemplateEnum.MAIL_AVAILABLE_SENDER.getValue(), language);
			mailNotificationServices.prepareAndSend(enclosure.getSender(), subjectSenderPassw, enclosure,
					NotificationTemplateEnum.MAIL_PASSWORD_SENDER.getValue(), language);
		}
		if (!publicLink) {
			sendToRecipients(enclosure, new String(subjectRecipientLang),
					NotificationTemplateEnum.MAIL_AVAILABLE_RECIPIENT.getValue(), metaDataRecipient, currentLanguage);
		}

		// ---
		redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
				EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.PAT.getCode(), -1);
		redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
				EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.PAT.getWord(), -1);
	}

	// Send mails to recipients
	public void sendToRecipients(Enclosure enclosure, String subject, String templateName,
			NewRecipient metaDataRecipient, Locale currentLanguage) throws MetaloadException {

		boolean fromPublicApi = SourceEnum.PUBLIC.getValue().equalsIgnoreCase(redisManager.getHgetString(
				RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()), EnclosureKeysEnum.SOURCE.getKey()));
		boolean sendToRecipient = Boolean.parseBoolean(redisManager.getHgetString(
				RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()), EnclosureKeysEnum.ENVOIMDPDEST.getKey()));

		Locale language = Locale.FRANCE;
		try {
			language = LocaleUtils.toLocale(RedisUtils.getEnclosureValue(redisManager, enclosure.getGuid(),
					EnclosureKeysEnum.LANGUAGE.getKey()));
		} catch (Exception eL) {
			LOGGER.error("Error while getting local", eL);
		}

		subject = subject + " " + enclosure.getSender();
		String subjectPassword = new String(subjectRecipientPassword);

		if (Locale.UK.equals(language)) {
			subjectPassword = new String(subjectRecipientPasswordEn);
		}

		if (StringUtils.isNotBlank(enclosure.getSubject())) {

			if (Locale.UK.equals(language)) {
				subject = subject.concat(": ").concat(enclosure.getSubject());
			} else {
				subject = subject.concat(" : ").concat(enclosure.getSubject());
			}
			subjectPassword = subjectPassword.concat(enclosure.getSubject());

		}
		List<Recipient> recipients = enclosure.getRecipients();
		if (!CollectionUtils.isEmpty(recipients)) {
			if (metaDataRecipient != null) {
				if (StringUtils.isNotBlank(metaDataRecipient.getMail())) {
					Recipient newRec = new Recipient();
					newRec.setMail(metaDataRecipient.getMail());
					newRec.setId(metaDataRecipient.getId());
					List<Recipient> newRecipientList = new ArrayList<>();
					newRecipientList.add(newRec);
					recipients = newRecipientList;
				}
			}

			RedisUtils.updateListOfPliReceived(redisManager, recipients.stream().filter(x -> !x.isSuppressionLogique())
					.map(x -> x.getMail()).collect(Collectors.toList()), enclosure.getGuid());

			for (Recipient recipient : recipients) {
				if (!recipient.isSuppressionLogique()) {
					boolean clientNotifRIZOMO = redisManager.sexists(
							RedisKeysEnum.FT_DOMAINS_MAILS_NOTIF_RIZOMO.getKey(""), recipient.getMail().toLowerCase());
					if (clientNotifRIZOMO) {
						try {
							HttpHeaders headers = new HttpHeaders();
							headers.setContentType(MediaType.APPLICATION_JSON);
							headers.set(rizomoHeaderToken, rizomoToken);
							NotificationContent notifContent = new NotificationContent();

							notifContent.setEmail(recipient.getMail().toLowerCase());
							notifContent.setTitle("[France transfert] pli reçu de "
									+ enclosure.getSender().toLowerCase() + " : " + enclosure.getSubject());
							notifContent.setContent(
									"Cliquez sur le lien pour accéder à votre pli dont le mot de passe est : "
											+ enclosure.getPassword());
							notifContent.setLink(mailNotificationServices.generateUrlForDownload(enclosure.getGuid(),
									recipient.getMail(), recipient.getId()));
							notifContent.setType("message");

							HttpEntity<NotificationContent> requestEntity = new HttpEntity<>(notifContent, headers);
							LOGGER.debug("Send to rizomo: {}", notifContent);
							ResponseEntity<JsonObject> rizomoResponse = restTemplate.postForEntity(rizomoNotifUrl,
									requestEntity, JsonObject.class);
							rizomoResponse.getBody();
							LOGGER.debug("Rizomo response: ", rizomoResponse.getBody());
						} catch (Exception e) {
							LOGGER.error("Cannot send to Rizomo: " + e.getMessage(), e);
							sendMailToRecipient(enclosure, subject, templateName, fromPublicApi, sendToRecipient,
									language, subjectPassword, recipient);
						}
					} else {
						sendMailToRecipient(enclosure, subject, templateName, fromPublicApi, sendToRecipient, language,
								subjectPassword, recipient);
					}
				}
			}
		}
	}

	private void sendMailToRecipient(Enclosure enclosure, String subject, String templateName, boolean fromPublicApi,
			boolean sendToRecipient, Locale language, String subjectPassword, Recipient recipient) {
		try {
			LOGGER.info("send email notification availble to recipient: {} for enclosure {}", recipient.getMail(),
					enclosure.getGuid());

			enclosure.setUrlDownload(mailNotificationServices.generateUrlForDownload(enclosure.getGuid(),
					recipient.getMail(), recipient.getId()));

			boolean clientResana = redisManager.sexists(RedisKeysEnum.FT_DOMAINS_MAILS_RESANA.getKey(""),
					recipient.getMail().toLowerCase());
			enclosure.setCkeckRESANA(clientResana);
			enclosure.setUrlResana("");
			if (clientResana) {
				enclosure
						.setUrlResana(MessageFormat.format(urlPatternResana, enclosure.getGuid(), recipient.getMail()));
			}
			boolean clientOSMOSE = redisManager.sexists(RedisKeysEnum.FT_DOMAINS_MAILS_OSMOSE.getKey(""),
					recipient.getMail().toLowerCase());
			enclosure.setCkeckOSMOSE(clientOSMOSE);
			enclosure.setUrlOsmose("");
			if (clientOSMOSE) {
				enclosure
						.setUrlOsmose(MessageFormat.format(urlPatternOsmose, enclosure.getGuid(), recipient.getMail()));
			}

			mailNotificationServices.prepareAndSend(recipient.getMail(), subject, enclosure, templateName, language);
			// Send mail from web or api if check
			if ((fromPublicApi && sendToRecipient) || !fromPublicApi) {
				mailNotificationServices.prepareAndSend(recipient.getMail(), subjectPassword, enclosure,
						NotificationTemplateEnum.MAIL_PASSWORD_RECIPIENT.getValue(), language);
			}
		} catch (Exception e) {
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
					EnclosureKeysEnum.STATUS_CODE.getKey(), StatutEnum.EEC.getCode(), -1);
			redisManager.hsetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosure.getGuid()),
					EnclosureKeysEnum.STATUS_WORD.getKey(), StatutEnum.EEC.getWord(), -1);
			LOGGER.error("Cannot send mail recipient mail {} for enclosure {}", recipient.getMail(),
					enclosure.getGuid());
		}
	}
}

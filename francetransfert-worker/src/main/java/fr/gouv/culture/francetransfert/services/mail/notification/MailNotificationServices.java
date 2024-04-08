/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.services.mail.notification;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import fr.gouv.culture.francetransfert.core.enums.EnclosureKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.utils.Base64CryptoService;
import fr.gouv.culture.francetransfert.model.Enclosure;
import fr.gouv.culture.francetransfert.security.WorkerException;
import fr.gouv.culture.francetransfert.services.mail.notification.enums.NotificationTemplateEnum;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;

@Component
public class MailNotificationServices {

	private static final Logger LOGGER = LoggerFactory.getLogger(MailNotificationServices.class);

//    properties mail France transfert SMTP
	@Value("${spring.mail.ftmail}")
	private String franceTransfertMail;

	@Value("${contact.mail:''}")
	private String franceTransfertContactMail;

	@Value("${url.download.api}")
	private String urlDownloadApi;
	@Value("${url.admin.page}")
	private String urlAdminPage;

	@Autowired
	private JavaMailSender emailSender;

	@Autowired
	private MailContentBuilder htmlBuilder;

	@Autowired
	Base64CryptoService base64CryptoService;

	@Autowired
	private RedisManager redisManager;

	public void prepareAndSend(String to, String subject, Object object, String templateName, Locale locale) {

		try {
			LOGGER.debug("start send emails for enclosure ");
			if (locale == null || locale.toString().isEmpty()) {
				locale = Locale.FRENCH;
			}
			templateName = templateName != null && !templateName.isEmpty() ? templateName
					: NotificationTemplateEnum.MAIL_TEMPLATE.getValue();
			JavaMailSenderImpl sender = new JavaMailSenderImpl();
			MimeMessage message = sender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(franceTransfertMail);
			helper.setTo(to);
			if (StringUtils.equalsIgnoreCase(NotificationTemplateEnum.MAIL_AVAILABLE_RECIPIENT.getValue(),
					templateName)) {
				Enclosure packageInfo = (Enclosure) object;
				String senderMail = packageInfo.getSender();
				helper.setReplyTo(senderMail);
			}

			if (StringUtils.isNotBlank(subject)) {
				helper.setSubject(MimeUtility.encodeText(subject, "utf-8", "B"));
			}

			String htmlContent = htmlBuilder.build(object, templateName, locale);
			helper.setText(htmlContent, true);
			emailSender.send(message);

		} catch (MessagingException | IOException e) {
			throw new WorkerException("Enclosure build error", e);
		}
	}

	public void prepareAndSend(String to, String subject, String body, String templateName, Locale locale) {
		try {
			LOGGER.debug("start send emails for enclosure ");
			templateName = templateName != null && !templateName.isEmpty() ? templateName
					: NotificationTemplateEnum.MAIL_TEMPLATE.getValue();
			JavaMailSenderImpl sender = new JavaMailSenderImpl();
			MimeMessage message = sender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(franceTransfertMail);
			helper.setTo(to);
			helper.setSubject(subject);
			String htmlContent = htmlBuilder.build(body, templateName, locale);
			helper.setText(htmlContent, true);
			emailSender.send(message);
		} catch (MessagingException e) {
			throw new WorkerException("Enclosure build error", e);
		}
	}

	public void prepareAndSendMailContact(String from, String subject, Object object, String templateName) {
		try {
			LOGGER.debug("start send emails contact ");
			templateName = templateName != null && !templateName.isEmpty() ? templateName
					: NotificationTemplateEnum.MAIL_TEMPLATE.getValue();
			JavaMailSenderImpl sender = new JavaMailSenderImpl();
			MimeMessage message = sender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(franceTransfertMail);
			helper.setTo(franceTransfertContactMail);
			helper.setSubject(subject);
			String htmlContent = htmlBuilder.build(object, templateName, Locale.FRENCH);
			helper.setText(htmlContent, true);
			emailSender.send(message);
		} catch (MessagingException | IOException e) {
			throw new WorkerException("formulaire contact build error", e);
		}
	}

	public String generateUrlForDownload(String enclosureId, String recipientMail, String recipientId) {
		try {
			Map<String, String> enclosureMap = redisManager
					.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
			String lang = enclosureMap.get(EnclosureKeysEnum.LANGUAGE.getKey());
			String urlDownload = urlDownloadApi + "?enclosure=" + enclosureId + "&recipient="
					+ base64CryptoService.base64Encoder(recipientMail) + "&token=" + recipientId;
			if (StringUtils.isNotBlank(lang)) {
				urlDownload = urlDownload + "&lang=" + lang.replace("_", "-");
			}
			return urlDownload;
		} catch (UnsupportedEncodingException e) {
			throw new WorkerException("Download url error", e);
		}
	}

	public String generateUrlPublicForDownload(String enclosureId) {
		String publicDownloadurl = urlDownloadApi + "download-info-public?enclosure=" + enclosureId;
		Map<String, String> enclosureMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
		String lang = enclosureMap.get(EnclosureKeysEnum.LANGUAGE.getKey());
		if (StringUtils.isNotBlank(lang)) {
			publicDownloadurl = publicDownloadurl + "&lang=" + lang.replace("_", "-");
		}
		return publicDownloadurl;
	}

	public boolean getPublicLink(String enclosureId) {
		Map<String, String> enclosureMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
		return Boolean.parseBoolean(enclosureMap.get(EnclosureKeysEnum.PUBLIC_LINK.getKey()));
	}

	public String generateUrlAdmin(String enclosureId) {
		Map<String, String> tokenMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ADMIN_TOKEN.getKey(enclosureId));
		String adminUrl = urlAdminPage + "?token=" + tokenMap.get(EnclosureKeysEnum.TOKEN.getKey()) + "&enclosure="
				+ enclosureId;
		Map<String, String> enclosureMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
		String lang = enclosureMap.get(EnclosureKeysEnum.LANGUAGE.getKey());
		if (StringUtils.isNotBlank(lang)) {
			adminUrl = adminUrl + "&lang=" + lang.replace("_", "-");
		}
		return adminUrl;

	}

	public void send(String to, String subject, String content) {
		try {
			LOGGER.debug("Start simple send");
			JavaMailSenderImpl sender = new JavaMailSenderImpl();
			MimeMessage message = sender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(franceTransfertMail);
			helper.setTo(to);
			if (StringUtils.isNotBlank(subject)) {
				helper.setSubject(MimeUtility.encodeText(subject, "utf-8", "B"));
			}
			String htmlContent = content;
			helper.setText(htmlContent, true);
			emailSender.send(message);
		} catch (MessagingException | IOException e) {
			throw new WorkerException("Simple send error", e);
		}
	}

}

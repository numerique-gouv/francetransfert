/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.services;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.gouv.culture.francetransfert.application.error.ErrorEnum;
import fr.gouv.culture.francetransfert.application.error.UnauthorizedAccessException;
import fr.gouv.culture.francetransfert.application.resources.model.ValidateCodeResponse;
import fr.gouv.culture.francetransfert.core.enums.EnclosureKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisQueueEnum;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.utils.RedisUtils;
import fr.gouv.culture.francetransfert.core.utils.StringUploadUtils;
import fr.gouv.culture.francetransfert.domain.exceptions.ConfirmationCodeException;
import fr.gouv.culture.francetransfert.domain.exceptions.DomainNotFoundException;
import fr.gouv.culture.francetransfert.domain.exceptions.MaxTryException;
import fr.gouv.culture.francetransfert.domain.exceptions.UploadException;

@Service
public class ConfirmationServices {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmationServices.class);

	@Value("${expire.confirmation.code.length}")
	private int lengthCode;

	@Value("${expire.confirmation.code}")
	private int secondsToExpireConfirmationCode;

	@Value("${expire.token.sender}")
	private int expireTokenSender;

	@Value("${application.cookies.domain}")
	private String applicationCookiesDomain;

	@Value("${enclosure.max.password.try}")
	private int maxTryCodeCount;

	@Autowired
	private RedisManager redisManager;

	@Autowired
	private StringUploadUtils stringUploadUtils;

	public void generateCodeConfirmation(String senderMail, String currentLanguage) {
		// generate confirmation code
		// verify code exist in REDIS for this mail : if not exist -> generate
		// confirmation code and insert in queue redis (send mail to the sender
		// enclosure with code)
		senderMail = senderMail.toLowerCase();
		if (stringUploadUtils.isValidEmail(senderMail)) {
			if (StringUtils.isBlank(redisManager.getString(
					RedisKeysEnum.FT_CODE_SENDER.getKey(RedisUtils.generateHashsha1(senderMail.toLowerCase()))))) {
				String confirmationCode = RandomStringUtils.randomNumeric(lengthCode);

				// insert confirmation code in REDIS
				redisManager.setNxString(RedisKeysEnum.FT_CODE_SENDER.getKey(RedisUtils.generateHashsha1(senderMail)),
						confirmationCode, secondsToExpireConfirmationCode);
				redisManager.deleteKey(RedisKeysEnum.FT_CODE_SENDER.getKey(senderMail));
				redisManager.setString(RedisKeysEnum.FT_CODE_TRY.getKey(RedisUtils.generateHashsha1(senderMail)), "0");
				LOGGER.info("sender: {} generated confirmation code in redis", senderMail);
				// insert in queue of REDIS: confirmation-code-mail" => SenderMail":"code" (
				// insert in queue to: send mail to sender in worker module)
				Long ttl = redisManager
						.ttl(RedisKeysEnum.FT_CODE_SENDER.getKey(RedisUtils.generateHashsha1(senderMail)));
				String ttltCodeConfirmation = ZonedDateTime.now(ZoneId.of("Europe/Paris")).plusSeconds(ttl).toString();
				redisManager.publishFT(RedisQueueEnum.CONFIRMATION_CODE_MAIL_QUEUE.getValue(),
						currentLanguage + ":" + senderMail + ":" + confirmationCode + ":" + ttltCodeConfirmation);
				LOGGER.info("sender: {} insert in queue rdis to send mail with confirmation code", senderMail);
				LOGGER.warn("msgtype: CONFIRMATION_REQUEST || sender: {}", senderMail);
			} else {
				String codeSent = redisManager.getString(
						RedisKeysEnum.FT_CODE_SENDER.getKey(RedisUtils.generateHashsha1(senderMail.toLowerCase())));
				redisManager.expire(RedisKeysEnum.FT_CODE_SENDER.getKey(RedisUtils.generateHashsha1(senderMail)),
						secondsToExpireConfirmationCode);
				Long ttl = redisManager
						.ttl(RedisKeysEnum.FT_CODE_SENDER.getKey(RedisUtils.generateHashsha1(senderMail)));
				String ttltCodeConfirmation = ZonedDateTime.now(ZoneId.of("Europe/Paris")).plusSeconds(ttl).toString();
				redisManager.publishFT(RedisQueueEnum.CONFIRMATION_CODE_MAIL_QUEUE.getValue(),
						currentLanguage + ":" + senderMail + ":" + codeSent + ":" + ttltCodeConfirmation);
				LOGGER.info("Code still valid for sender {} with ttl {}", senderMail, ttl);
			}
		} else {
			throw new UploadException(ErrorEnum.SENDER_MAIL_INVALID.getValue(),
					"SenderEmail format is not valid (connect)");
		}

	}

	public ValidateCodeResponse validateCodeConfirmationAndGenerateToken(String senderMail, String code)
			throws ConfirmationCodeException {
		// validate confirmation code
		validateCodeConfirmation(senderMail, code);
		try {
			/*
			 * genarate and insert in REDIS :(GUID && timpStamp cokies) per sender by
			 * internet browser add token validity sender to Redis. Token form :
			 * "sender:senderMail:token" => SET ["GUID:time-stamp"] exemple :
			 * "sender:test@gouv.fr:token" => SET
			 * [e4cce869-6f3d-4e10-900a-74299602f460:2018-01-21T12:01:34.519, ..]
			 */
			String token = RedisUtils.generateGUID();
			String tokenKey = RedisKeysEnum.FT_TOKEN_SENDER.getKey(senderMail.toLowerCase()) + ":" + token;
			redisManager.deleteKey(
					RedisKeysEnum.FT_CODE_SENDER.getKey(RedisUtils.generateHashsha1(senderMail.toLowerCase())));
			redisManager
					.deleteKey(RedisKeysEnum.FT_CODE_TRY.getKey(RedisUtils.generateHashsha1(senderMail.toLowerCase())));
			redisManager.saddString(tokenKey, token);
			int secondToExpire = expireTokenSender;
			redisManager.expire(tokenKey, secondToExpire);
			LOGGER.info("sender: {} generated token: {} ", senderMail, token);
			ValidateCodeResponse response = new ValidateCodeResponse(senderMail, token);
			return response;
		} catch (Exception e) {
			String uuid = UUID.randomUUID().toString();
			throw new UploadException(
					ErrorEnum.TECHNICAL_ERROR.getValue() + "during code validation : " + e.getMessage(), uuid, e);
		}
	}

	public void validateCodeConfirmation(String senderMail, String code) throws ConfirmationCodeException {
		LOGGER.info("verify validy confirmation code");
		String redisCode = redisManager
				.getString(RedisKeysEnum.FT_CODE_SENDER.getKey(RedisUtils.generateHashsha1(senderMail.toLowerCase())));
		int tryCount = 0;
		try {
			tryCount = Integer.parseInt(
					redisManager.getString(RedisKeysEnum.FT_CODE_TRY.getKey(RedisUtils.generateHashsha1(senderMail))));
		} catch (Exception e) {
			throw new DomainNotFoundException("Code Invalid : " + senderMail, e);
		}

		// Si try > au maxTry on delete le code de confirmation
		if (tryCount >= maxTryCodeCount) {
			deleteConfirmationCode(senderMail);
		}

		// Si le code est invalide on incr/delete si superieur au max try et throw
		if (!code.equals(redisCode)) {
			if ((tryCount + 1) >= maxTryCodeCount) {
				deleteConfirmationCode(senderMail);
			} else {
				tryCount++;
				LOGGER.error("error code sender: this code: {} is not validated for this sender mail {}", code,
						senderMail);
				redisManager.setString(RedisKeysEnum.FT_CODE_TRY.getKey(RedisUtils.generateHashsha1(senderMail)),
						Integer.toString(tryCount));
				throw new ConfirmationCodeException(ErrorEnum.CONFIRMATION_CODE_ERROR.getValue(), tryCount);
			}
			// Si le code est valide et try < au max on valide
		} else {
			redisManager.setString(RedisKeysEnum.FT_CODE_TRY.getKey(RedisUtils.generateHashsha1(senderMail)), "0");
			LOGGER.info("sender: {} valid code: {} ", senderMail, code);
			LOGGER.warn("msgtype: CONFIRMATION_OK || sender: {}", senderMail);
		}
	}

	public void validateTokenAndEnclosure(String senderMail, String token, String enclosureId)
			throws MetaloadException {
		// verify token in redis
		LOGGER.debug("check token for sender mail {}", senderMail);
		redisManager.validateToken(senderMail, token);
		String senderEnclosureMail = RedisUtils.getEmailSenderEnclosure(redisManager, enclosureId);
		if (!StringUtils.equalsIgnoreCase(senderMail, senderEnclosureMail)) {
			throw new MetaloadException("Invalid token");
		}

	}

	public void validateToken(String senderMail, String token) throws MetaloadException {
		// verify token in redis
		LOGGER.debug("check token for sender mail {}", senderMail);
		redisManager.validateToken(senderMail, token);
	}

	public void validateAdminToken(String enclosureId, String token, String senderMail) {
		Map<String, String> tokenMap = redisManager.hmgetAllString(RedisKeysEnum.FT_ADMIN_TOKEN.getKey(enclosureId));
		if (tokenMap != null) {
			if (!token.equals(tokenMap.get(EnclosureKeysEnum.TOKEN.getKey()))) {
				if (StringUtils.isNotBlank(senderMail)) {
					try {
						redisManager.validateToken(senderMail.toLowerCase(), token);
						String senderEnclosureMail = RedisUtils.getEmailSenderEnclosure(redisManager, enclosureId);
						if (!StringUtils.equalsIgnoreCase(senderMail, senderEnclosureMail)) {
							throw new UnauthorizedAccessException("Invalid Token");
						}
						redisManager.extendTokenValidity(senderMail.toLowerCase(), token);
					} catch (Exception e) {
						throw new UnauthorizedAccessException("Invalid Token");
					}
				} else {
					throw new UnauthorizedAccessException("Invalid Token");
				}
			}
		} else {
			throw new UnauthorizedAccessException("Invalid Token");
		}
	}

	private void deleteConfirmationCode(String senderMail) {
		redisManager.deleteKey(RedisKeysEnum.FT_CODE_SENDER.getKey(RedisUtils.generateHashsha1(senderMail)));
		redisManager.deleteKey(RedisKeysEnum.FT_CODE_TRY.getKey(RedisUtils.generateHashsha1(senderMail)));
		throw new MaxTryException("Unauthorized");
	}

}

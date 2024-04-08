/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.services;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import fr.gouv.culture.francetransfert.application.error.ErrorEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisQueueEnum;
import fr.gouv.culture.francetransfert.core.enums.TypeStat;
import fr.gouv.culture.francetransfert.core.model.RateRepresentation;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.core.utils.Base64CryptoService;
import fr.gouv.culture.francetransfert.domain.exceptions.DownloadException;

@Service
public class RateServices {
	private static final Logger LOGGER = LoggerFactory.getLogger(RateServices.class);

	@Autowired
	RedisManager redisManager;

	@Autowired
	Base64CryptoService base64CryptoService;

	public boolean createSatisfactionFT(RateRepresentation rateRepresentation) throws DownloadException {
		try {

			if (null == rateRepresentation) {
				String uuid = UUID.randomUUID().toString();
				throw new DownloadException("rateRepresentation is null", uuid);
			}
			String domain = "";
			if (StringUtils.isNotBlank(rateRepresentation.getMailAdress())) {
				domain = rateRepresentation.getMailAdress().split("@")[1];
			}
			rateRepresentation.setDate(LocalDate.now().toString());
			rateRepresentation.setDomain(domain);
			rateRepresentation.setHashMail(null);
			rateRepresentation.setMailAdress(null);
			rateRepresentation.setType(TypeStat.DOWNLOAD_SATISFACTION);
			String jsonInString = new Gson().toJson(rateRepresentation);
			redisManager.publishFT(RedisQueueEnum.SATISFACTION_QUEUE.getValue(), jsonInString);
			return true;
		} catch (Exception e) {
			String uuid = UUID.randomUUID().toString();
			throw new DownloadException(ErrorEnum.TECHNICAL_ERROR.getValue(), uuid, e);
		}
	}
}

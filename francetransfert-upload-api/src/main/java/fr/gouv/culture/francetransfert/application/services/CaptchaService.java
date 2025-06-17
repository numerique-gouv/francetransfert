/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import fr.gouv.culture.francetransfert.core.enums.CaptchaTypeEnum;
import fr.gouv.culture.francetransfert.domain.soap.EnvelopeUtils;

@Service
public class CaptchaService {

	@Autowired
	private RestTemplate restTemplate;

	@Value("${captcha.url:''}")
	private String capchatUrl;

	public boolean checkCaptcha(String challengeId, String userResponse, CaptchaTypeEnum captchaType) {

		String postData = EnvelopeUtils.generateCheckCaptcha(challengeId, userResponse, captchaType);
		ResponseEntity<String> soapResponse = restTemplate.postForEntity(capchatUrl, postData, String.class);
		if (soapResponse.getBody().contains("<return>true</return>")) {
			return true;
		}
		return false;
	}

}

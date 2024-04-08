/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.services.mail;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import fr.gouv.culture.francetransfert.model.RizomoSynchro;
import fr.gouv.culture.francetransfert.model.SnapResponse;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SnapService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SnapService.class);

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	RedisManager redisManager;

	@Value("${osmose.url:}")
	private String osmoseUrl;

	@Value("${osmose.auth.token:test}")
	private String osmoseToken;

	@Value("${osmose.auth.header:X-Api-Key}")
	private String osmoseHeaderToken;

	@Value("${osmose.mock.defaultValue}")
	private String[] osmoseMockValue;

	@Value("${resana.url:}")
	private String resanaUrl;

	@Value("${resana.auth.token:test}")
	private String resanaToken;

	@Value("${resana.auth.header:X-Api-Key}")
	private String resanaHeaderToken;

	@Value("${resana.mock.defaultValue}")
	private String[] resanaMockValue;
	
    @Value("${rizomo.url:}")
    private String rizomoUrl;

    @Value("${rizomo.auth.token:test}")
    private String rizomoToken;

    @Value("${rizomo.auth.header:X-Api-Key}")
    private String rizomoHeaderToken;
    
	@Value("${rizomo.mock.defaultValue}")
	private String[] rizomoMockValue;

    @Value("${rizomo.application}")
    private String application;
    
	@Value("${snap.mock:true}")
	private boolean snapMock;

	public void updateOsmose() {
		LOGGER.info("Start Osmose Sync");
		try {
			if (snapMock) {
				redisManager.deleteKey(RedisKeysEnum.FT_DOMAINS_MAILS_OSMOSE.getKey(""));
				redisManager.saddStrings(RedisKeysEnum.FT_DOMAINS_MAILS_OSMOSE.getKey(""), osmoseMockValue);
			} else {
				HttpHeaders headers = new HttpHeaders();
				headers.set(osmoseHeaderToken, osmoseToken);
				HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
				ResponseEntity<SnapResponse> templateReturn = restTemplate.exchange(osmoseUrl, HttpMethod.GET,
						requestEntity, SnapResponse.class);
				SnapResponse response = templateReturn.getBody();
				if (response != null && CollectionUtils.isNotEmpty(response.getMails())) {
					redisManager.deleteKey(RedisKeysEnum.FT_DOMAINS_MAILS_OSMOSE.getKey(""));
					redisManager.saddStrings(RedisKeysEnum.FT_DOMAINS_MAILS_OSMOSE.getKey(""),
							response.getMails().toArray(new String[response.getMails().size()]));
				}
			}

		} catch (Exception e) {
			LOGGER.error("Error while syncing osmose", e);
		}
		LOGGER.info("End Osmose Sync");
	}

	public void updateResana() {
		LOGGER.info("Start Resana Sync");
		try {
			if (snapMock) {
				redisManager.deleteKey(RedisKeysEnum.FT_DOMAINS_MAILS_RESANA.getKey(""));
				redisManager.saddStrings(RedisKeysEnum.FT_DOMAINS_MAILS_RESANA.getKey(""), resanaMockValue);
			} else {
				HttpHeaders headers = new HttpHeaders();
				headers.set(resanaHeaderToken, resanaToken);
				HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
				ResponseEntity<SnapResponse> templateReturn = restTemplate.exchange(resanaUrl, HttpMethod.GET,
						requestEntity, SnapResponse.class);
				SnapResponse response = templateReturn.getBody();
				if (response != null && CollectionUtils.isNotEmpty(response.getMails())) {
					redisManager.deleteKey(RedisKeysEnum.FT_DOMAINS_MAILS_RESANA.getKey(""));
					redisManager.saddStrings(RedisKeysEnum.FT_DOMAINS_MAILS_RESANA.getKey(""),
							response.getMails().toArray(new String[response.getMails().size()]));
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error while syncing resana", e);
		}
		LOGGER.info("End Resana Sync");
	}
	
	   public void updateRizomo() {
	        LOGGER.info("Start rizomo Sync");
	        try {
	            if (snapMock) {
	                redisManager.deleteKey(RedisKeysEnum.FT_DOMAINS_MAILS_NOTIF_RIZOMO.getKey(""));
	                redisManager.saddStrings(RedisKeysEnum.FT_DOMAINS_MAILS_NOTIF_RIZOMO.getKey(""), rizomoMockValue);
	            } else {
	                HttpHeaders headers = new HttpHeaders();
	                headers.set(rizomoHeaderToken, rizomoToken);
	                HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
	                ResponseEntity<RizomoSynchro> templateReturn = restTemplate.exchange(rizomoUrl, HttpMethod.GET,
	                        requestEntity, RizomoSynchro.class, application);
	                RizomoSynchro response = templateReturn.getBody();
	                if (response != null && CollectionUtils.isNotEmpty(response.getData())) {
	                    redisManager.deleteKey(RedisKeysEnum.FT_DOMAINS_MAILS_NOTIF_RIZOMO.getKey(""));
	                    redisManager.saddStrings(RedisKeysEnum.FT_DOMAINS_MAILS_NOTIF_RIZOMO.getKey(""),
	                            response.getData().toArray(new String[response.getData().size()]));
	                }
	            }
	        } catch (Exception e) {
	            LOGGER.error("Error while syncing rizomo", e);
	        }
	        LOGGER.info("End rizomo Sync");
	    }

}

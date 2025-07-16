/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import fr.gouv.culture.francetransfert.application.resources.model.ConfigRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.ConfigUpdate;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.services.MimeService;
import fr.gouv.culture.francetransfert.core.services.RedisManager;

@Service
public class ConfigService {

	@Value("${extension.name}")
	private List<String> extensionList;

	@Value("${mimetype.front}")
	private List<String> mimeList;

	@Value("${agentconnect.enabled:false}")
	private boolean agentConnect;

	@Value("${agentconnect.issuerUrl:}")
	private String issuerUrl;

	@Value("${agentconnect.clientId:}")
	private String clientId;

	@Autowired
	MimeService mimeService;

	@Autowired
	private RedisManager redisManager;

	public ConfigRepresentation getConfig() {
		String jsonInString = redisManager.getString(RedisKeysEnum.FT_CONFIG.getFirstKeyPart());
		Map<String, String> messages = new Gson().fromJson(jsonInString, Map.class);
		return ConfigRepresentation.builder().extension(extensionList).mimeType(mimeList).agentConnect(agentConnect)
				.clientId(clientId).issuerUrl(issuerUrl).messages(messages).build();
	}

	public void updateConfig(ConfigUpdate configUpdate) {
		String jsonInString = new Gson().toJson(configUpdate.getMessages());
		redisManager.setString(RedisKeysEnum.FT_CONFIG.getFirstKeyPart(), jsonInString);
	}

}

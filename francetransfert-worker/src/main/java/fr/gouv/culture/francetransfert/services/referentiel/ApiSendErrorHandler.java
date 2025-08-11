/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.services.referentiel;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;

public class ApiSendErrorHandler implements ResponseErrorHandler {

	Logger LOGGER = LoggerFactory.getLogger(ApiSendErrorHandler.class);

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return new DefaultResponseErrorHandler().hasError(response);
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {

		if (response.getStatusCode().is5xxServerError() || response.getStatusCode().is4xxClientError()) {
			// http status code e.g. `500 INTERNAL_SERVER_ERROR`
			LOGGER.error("API SEND ERROR {} , {}", response.getStatusCode(), response.getBody());
		}
	}
}

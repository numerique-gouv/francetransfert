/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.services.referentiel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import fr.gouv.culture.francetransfert.model.RefDomainResponse;
import fr.gouv.culture.francetransfert.security.WorkerException;

@Component
public class RestClientUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(RestClientUtils.class);

	private static final ArrayList<HttpMessageConverter<?>> converters = new ArrayList<>(
			Arrays.asList(new MappingJackson2HttpMessageConverter(), new ResourceHttpMessageConverter(),
					new FormHttpMessageConverter(), new StringHttpMessageConverter()));

	@Autowired
	private RestTemplateBuilder restTemplateBuilder;

	/**
	 * Referentiel Get domains Rest call
	 *
	 * @param token
	 * @param requestUri
	 * @param httpMethod
	 * @return
	 */
	public List<String> getDomain(String token, String requestUri, HttpMethod httpMethod) {

		ResponseEntity<RefDomainResponse> response = null;
		try {

			LOGGER.info("worker LaSuite domain update from [{}]", requestUri);
			RestTemplate restTemplate = restTemplateBuilder.messageConverters(converters)
					.errorHandler(new ApiSendErrorHandler()).build();

			response = restTemplate.exchange(requestUri, httpMethod,
					getHttpEntityBearerCredentials(null, token),
					RefDomainResponse.class);
		} catch (HttpMessageNotReadableException e) {
			LOGGER.error("Worker LaSuite domain update ERROR {} ", e.getMessage(), e);
		}

		return response.getBody().getItems();
	}

	public void sendStat(String requestUri, String token, HttpMethod httpMethod, File file, String headerName) {

		ResponseEntity response = null;
		try {

			LOGGER.info("worker send stat to {}", requestUri);

			RestTemplate restTemplate = restTemplateBuilder.messageConverters(converters)
					.errorHandler(new ApiSendErrorHandler()).build();

			HttpHeaders headers = new HttpHeaders();
			headers.add(headerName, token);
			headers.add("Content-Disposition", "attachment; filename=" + file.getName());
			headers.setContentType(MediaType.parseMediaType("text/csv"));

			String fileContent = Files.readString(file.toPath());
			HttpEntity<String> requestEntity = new HttpEntity<>(fileContent, headers);

			LOGGER.info("Worker sendfile to stat : " + file.getName());
			response = restTemplate.exchange(requestUri, httpMethod, requestEntity, Object.class);

			LOGGER.debug("stat response : {}", response.getBody());

			if (!response.getStatusCode().equals(HttpStatus.OK)) {
				LOGGER.error("stat response : {}", response.getBody());
				throw new WorkerException("error while sending file" + response.toString());
			}

		} catch (HttpMessageNotReadableException | IOException e) {
			LOGGER.error("Worker stat send ERROR {} ", e.getMessage(), e);
			throw new WorkerException("stat error", e);
		}

		return;
	}

	private <T> HttpEntity<?> getHttpEntityBearerCredentials(T reqBody,
			String headerValue) {

		return new HttpEntity<>(reqBody, getHeadersWithCredentials("Authorization", "Bearer " + headerValue));
	}

	private <T> HttpEntity<?> getHttpEntity(T reqBody) {

		return new HttpEntity<>(reqBody, getHeaders());
	}

	private HttpHeaders getHeadersWithCredentials(String headerName, String headerValue) {

		HttpHeaders headers = new HttpHeaders();
		headers.add(headerName, headerValue);
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		return headers;
	}

	private HttpHeaders getHeaders() {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		return headers;
	}

}

/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.services.ignimission;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import fr.gouv.culture.francetransfert.model.IgnimissionAuthenticationResponse;
import fr.gouv.culture.francetransfert.model.IgnimissionDomainParameter;
import fr.gouv.culture.francetransfert.model.IgnimissionDomainResponse;
import fr.gouv.culture.francetransfert.model.IgnimissionParameter;
import fr.gouv.culture.francetransfert.security.WorkerException;

@Component
public class RestClientUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(RestClientUtils.class);

	private static final ArrayList<HttpMessageConverter<?>> converters = new ArrayList<>(
			Arrays.asList(new MappingJackson2HttpMessageConverter(), new ResourceHttpMessageConverter(),
					new FormHttpMessageConverter()));

	@Autowired
	private RestTemplateBuilder restTemplateBuilder;

	/**
	 * Ignimission Authentication REST CALL
	 *
	 * @param parameter
	 * @param requestUri
	 * @param httpMethod
	 * @return
	 */
	public IgnimissionAuthenticationResponse getAuthentication(IgnimissionParameter parameter, String requestUri,
			HttpMethod httpMethod) {

		try {
			LOGGER.info("================================> Ignimission : get authentication token from [{}] ",
					requestUri);
			RestTemplate restTemplate = restTemplateBuilder.messageConverters(new MappingJackson2HttpMessageConverter())
					.errorHandler(new IgnimissionErrorHandler()).build();

			ResponseEntity<IgnimissionAuthenticationResponse> response = restTemplate.exchange(requestUri, httpMethod,
					getHttpEntity(parameter), IgnimissionAuthenticationResponse.class);
			return response.getBody();

		} catch (HttpMessageNotReadableException e) {
			LOGGER.error("Ignimission AUth CALL ERROR {} ", e.getMessage(), e);
		}

		return null;
	}

	/**
	 * Ignimission Get domains Rest call
	 *
	 * @param ignimissionDomainParameter
	 * @param token
	 * @param requestUri
	 * @param httpMethod
	 * @return
	 */
	public IgnimissionDomainResponse getAsamExtensions(IgnimissionDomainParameter ignimissionDomainParameter,
			String token, String requestUri, HttpMethod httpMethod) {

		ResponseEntity<IgnimissionDomainResponse[]> response = null;
		try {

			LOGGER.info("================================> worker Ignimission domain update from [{}]", requestUri);
			RestTemplate restTemplate = restTemplateBuilder.messageConverters(converters)
					.errorHandler(new IgnimissionErrorHandler()).build();

			response = restTemplate.exchange(requestUri, httpMethod,
					getHttpEntityWithCredentials(ignimissionDomainParameter, token), IgnimissionDomainResponse[].class);
		} catch (HttpMessageNotReadableException e) {
			LOGGER.error("Worker Ignimission domain update ERROR {} ", e.getMessage(), e);
		}

		return Objects.nonNull(response) ? Arrays.stream(response.getBody()).findFirst().orElse(null) : null;
	}

	public void sendIgniStat(String token, String requestUri, HttpMethod httpMethod, File file, String idStat) {

		ResponseEntity response = null;
		try {

			LOGGER.info("================================> worker Ignimission domain update from [{}]", requestUri);

			RestTemplate restTemplate = restTemplateBuilder.messageConverters(converters)
					.errorHandler(new IgnimissionErrorHandler()).build();

			// Setting Auth Header and type MULTIPART_FORM not JSON content type
			HttpHeaders headers = new HttpHeaders();
			headers.add("Authorization", "Bearer " + token);
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
			// Building multipart form
			MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			// Parser need a FileSystemResource or an InputStreamSource
			FileSystemResource fileRessource = new FileSystemResource(file);
			map.add("file", fileRessource);
			// Unable to send Integer transfom it to string
			map.add("datasource_id", idStat + "");

			HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);

			LOGGER.info("Worker sendfile to ignimission : " + file.getName());
			response = restTemplate.exchange(requestUri, httpMethod, requestEntity, Object.class);
			if (!response.getStatusCode().equals(HttpStatus.OK)) {
				throw new WorkerException("Ignimission error while sending file" + response.toString());
			}

		} catch (HttpMessageNotReadableException e) {
			LOGGER.error("Worker Ignimission domain update ERROR {} ", e.getMessage(), e);
			throw new WorkerException("Ignimission error" + response.toString());
		}

		return;
	}

	private <T> HttpEntity<?> getHttpEntityWithCredentials(T reqBody, String token) {

		return new HttpEntity<>(reqBody, getHeadersWithCredentials(token));
	}

	private <T> HttpEntity<?> getHttpEntity(T reqBody) {

		return new HttpEntity<>(reqBody, getHeaders());
	}

	private HttpHeaders getHeadersWithCredentials(String token) {

		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + token);
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

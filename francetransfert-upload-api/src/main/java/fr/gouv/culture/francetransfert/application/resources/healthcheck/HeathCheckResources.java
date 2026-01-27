/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.healthcheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.gouv.culture.francetransfert.application.error.UnauthorizedAccessException;
import fr.gouv.culture.francetransfert.application.services.HealthCheckService;
import fr.gouv.culture.francetransfert.core.model.HealthCheckRepresentation;
import fr.gouv.culture.francetransfert.domain.exceptions.UploadException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/api-private/heathcheck")
@Tag(name = "HeathCheck")
public class HeathCheckResources {

	private static final Logger LOGGER = LoggerFactory.getLogger(HeathCheckResources.class);

	@Autowired
	private HealthCheckService healthCheckService;

	@Value("${healthcheck.api.key:''}")
	String apiKeyConfig;

	@GetMapping("/")
	@Operation(method = "Get", description = "HeathCheck")
	public ResponseEntity<HealthCheckRepresentation> healthCheck(@RequestHeader("X-Api-Key") String apiKey,
			HttpServletRequest request) throws UploadException {

		if (apiKeyConfig.equals(apiKey)) {
			HealthCheckRepresentation heathStatus = healthCheckService.healthCheck();
			if (heathStatus.isFtError()) {
				return new ResponseEntity<HealthCheckRepresentation>(heathStatus, HttpStatus.SERVICE_UNAVAILABLE);
			}
			return new ResponseEntity<HealthCheckRepresentation>(heathStatus, HttpStatus.OK);
		}
		throw new UnauthorizedAccessException("Invalid Header");
	}

}

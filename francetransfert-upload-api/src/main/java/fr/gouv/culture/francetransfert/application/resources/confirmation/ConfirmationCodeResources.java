/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.confirmation;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.gouv.culture.francetransfert.application.error.ErrorEnum;
import fr.gouv.culture.francetransfert.application.resources.model.ValidateCodeResponse;
import fr.gouv.culture.francetransfert.application.services.ConfirmationServices;
import fr.gouv.culture.francetransfert.domain.exceptions.UploadException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api-private/confirmation-module")
@Tag(name = "Confirmation code resources")
@Validated
public class ConfirmationCodeResources {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmationCodeResources.class);

	@Autowired
	private ConfirmationServices confirmationServices;

	@GetMapping("/generate-code")
	@Operation(method = "GET", description = "Generate code")
	public void generateCode(HttpServletResponse response, @RequestParam("senderMail") String senderMail,
			@RequestParam("currentLanguage") String currentLanguage) throws UploadException {
		try {
			confirmationServices.generateCodeConfirmation(senderMail.toLowerCase(), currentLanguage);
			response.setStatus(HttpStatus.OK.value());
		} catch (Exception e) {
			String uuid = UUID.randomUUID().toString();
			throw new UploadException(ErrorEnum.TECHNICAL_ERROR.getValue() + " generating code : " + e.getMessage(),
					uuid, e);
		}
	}

	@GetMapping("/validate-code")
	@Operation(method = "GET", description = "validate code")
	public ValidateCodeResponse validateCode(HttpServletResponse response,
			@RequestParam("senderMail") String senderMail, @RequestParam("code") String code,
			@RequestParam("currentLanguage") String currentLanguage) {
		return confirmationServices.validateCodeConfirmationAndGenerateToken(senderMail.toLowerCase(), code.trim());
	}
}

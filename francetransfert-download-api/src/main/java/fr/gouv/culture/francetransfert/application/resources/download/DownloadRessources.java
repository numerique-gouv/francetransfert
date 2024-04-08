/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.download;

import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.gouv.culture.francetransfert.application.error.UnauthorizedAccessException;
import fr.gouv.culture.francetransfert.application.resources.model.Download;
import fr.gouv.culture.francetransfert.application.resources.model.DownloadPasswordMetaData;
import fr.gouv.culture.francetransfert.application.resources.model.DownloadRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.ValidatePasswordMetaData;
import fr.gouv.culture.francetransfert.application.resources.model.ValidatePasswordRepresentation;
import fr.gouv.culture.francetransfert.application.services.ConfirmationServices;
import fr.gouv.culture.francetransfert.application.services.DownloadServices;
import fr.gouv.culture.francetransfert.application.services.RateServices;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.exception.StatException;
import fr.gouv.culture.francetransfert.core.exception.StorageException;
import fr.gouv.culture.francetransfert.core.model.RateRepresentation;
import fr.gouv.culture.francetransfert.domain.exceptions.DownloadException;
import fr.gouv.culture.francetransfert.domain.exceptions.ExpirationEnclosureException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api-private/download-module")
@Tag(name = "Download resources")
@Validated
public class DownloadRessources {

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadRessources.class);

	@Autowired
	DownloadServices downloadServices;

	@Autowired
	private RateServices rateServices;

	@Autowired
	private ConfirmationServices confirmationServices;

	@PostMapping("/generate-download-url")
	@Operation(method = "POST", description = "Generate download URL ")
	public Download generateDownloadUrlWithPassword(@RequestBody DownloadPasswordMetaData downloadMeta)
			throws ExpirationEnclosureException, UnsupportedEncodingException, MetaloadException, StorageException {
		LOGGER.info("start generate download URL ");
		return downloadServices.generateDownloadUrl(downloadMeta);

	}

	@PostMapping("/generate-download-url-public")
	@Operation(method = "POST", description = "Generate download public URL ")
	public Download generateDownloadPublicUrlWithPassword(@RequestBody DownloadPasswordMetaData downloadMeta)
			throws UnauthorizedAccessException, UnsupportedEncodingException, MetaloadException {
		LOGGER.info("start generate download URL ");
		downloadServices.validatePublic(downloadMeta.getEnclosure());
		return downloadServices.generatePublicDownload(downloadMeta.getEnclosure(), downloadMeta.getPassword());
	}

	@PostMapping("/validate-password")
	@Operation(method = "POST", description = "Validate password")
	public ValidatePasswordRepresentation validatePassword(@RequestBody @Valid ValidatePasswordMetaData metaData)
			throws Exception {
		ValidatePasswordRepresentation representation = new ValidatePasswordRepresentation();
		try {
			String recipientId = downloadServices.getRecipientId(metaData.getEnclosureId(), metaData.getRecipientId());
			downloadServices.validatePassword(metaData.getEnclosureId(), metaData.getPassword(), recipientId);
			representation.setValid(true);
		} catch (Exception e) {
			representation.setValid(false);
			throw e;
		}
		return representation;
	}

	@GetMapping("/download-info")
	@Operation(method = "GET", description = "Download Info without URL ")
	public DownloadRepresentation downloadinfo(HttpServletResponse response,
			@RequestParam("enclosure") String enclosure, @RequestParam("recipient") String recipient,
			@RequestParam("token") String token)
			throws UnsupportedEncodingException, ExpirationEnclosureException, MetaloadException, StorageException {
		LOGGER.info("start donlowad info ");
		DownloadRepresentation downloadRepresentation = downloadServices.getDownloadInfo(enclosure, token, recipient);
		response.setStatus(HttpStatus.OK.value());
		return downloadRepresentation;
	}

	@GetMapping("/download-info-connect")
	@Operation(method = "GET", description = "Download Info without URL ")
	public DownloadRepresentation downloadinfoConnect(HttpServletResponse response,
			@RequestParam("enclosure") String enclosure, @RequestParam("token") String token,
			@RequestParam("recipient") String recipient)
			throws UnsupportedEncodingException, ExpirationEnclosureException, MetaloadException, StorageException {
		confirmationServices.validateToken(recipient.toLowerCase(), token);
		LOGGER.info("start donwlowad info connect for recipient: {} and enclosure: {}", recipient, enclosure);
		DownloadRepresentation downloadRepresentation = downloadServices.getDownloadInfoConnect(enclosure, recipient);
		response.setStatus(HttpStatus.OK.value());
		return downloadRepresentation;
	}

	@RequestMapping(value = "/satisfaction", method = RequestMethod.POST)
	@Operation(method = "POST", description = "Rates the app on a scvale of 1 to 4")
	public boolean createSatisfactionFT(HttpServletResponse response,
			@Valid @RequestBody RateRepresentation rateRepresentation) throws DownloadException {
		LOGGER.info("start Satisfaction ");
		return rateServices.createSatisfactionFT(rateRepresentation);
	}

	@GetMapping("/download-count-public")
	public String getDownloadCount(@RequestParam("enclosure") String enclosure, @RequestParam("token") String token)
			throws DownloadException {
		downloadServices.validateToken(enclosure, token);
		return downloadServices.getNumberOfDownloadPublic(enclosure);
	}

	@GetMapping("/download-info-public")
	public DownloadRepresentation downloadInfoPublic(HttpServletResponse response,
			@RequestParam("enclosure") String enclosure)
			throws UnauthorizedAccessException, ExpirationEnclosureException, MetaloadException {
		LOGGER.info("start download info public ");
		downloadServices.validatePublic(enclosure);
		DownloadRepresentation downloadRepresentation = downloadServices.getDownloadInfoPublic(enclosure);
		response.setStatus(HttpStatus.OK.value());
		return downloadRepresentation;
	}

	@PostMapping("/telechargerPliTest")
	@Operation(method = "POST", description = "telechargerPli")
	public String packageDownload(HttpServletResponse response, HttpServletRequest request,
			@RequestParam(value = "idPli", required = true) String enclosureId,
			@RequestParam(value = "courrielExpediteur", required = true) String senderMail,
			@RequestParam(value = "motDePasse", required = false) String motDePasse)
			throws MetaloadException, StatException {

		return senderMail;
	}

}

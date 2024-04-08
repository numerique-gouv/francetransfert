/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.upload;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fr.gouv.culture.francetransfert.application.error.UnauthorizedAccessException;
import fr.gouv.culture.francetransfert.application.resources.model.AddNewRecipientRequest;
import fr.gouv.culture.francetransfert.application.resources.model.ConfigRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.DateUpdateRequest;
import fr.gouv.culture.francetransfert.application.resources.model.DeleteRequest;
import fr.gouv.culture.francetransfert.application.resources.model.EnclosureRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.FileInfoRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.FranceTransfertDataRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.ValidateCodeResponse;
import fr.gouv.culture.francetransfert.application.services.ConfigService;
import fr.gouv.culture.francetransfert.application.services.ConfirmationServices;
import fr.gouv.culture.francetransfert.application.services.RateServices;
import fr.gouv.culture.francetransfert.application.services.UploadServices;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.exception.StorageException;
import fr.gouv.culture.francetransfert.core.model.FormulaireContactData;
import fr.gouv.culture.francetransfert.core.model.RateRepresentation;
import fr.gouv.culture.francetransfert.domain.exceptions.UploadException;
import fr.gouv.culture.francetransfert.validator.EmailsFranceTransfert;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api-private/upload-module")
@Tag(name = "Upload resources")
@Validated
public class UploadResources {

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadResources.class);

	@Autowired
	private UploadServices uploadServices;

	@Autowired
	private ConfigService configService;

	@Autowired
	private RateServices rateServices;

	@Autowired
	private ConfirmationServices confirmationServices;

	@GetMapping("/upload")
	@Operation(method = "GET", description = "Upload")
	public boolean chunkExists(HttpServletResponse response, @RequestParam("flowChunkNumber") int flowChunkNumber,
			@RequestParam("flowChunkSize") int flowChunkSize,
			@RequestParam("flowCurrentChunkSize") int flowCurrentChunkSize,
			@RequestParam("flowTotalSize") int flowTotalSize, @RequestParam("flowIdentifier") String flowIdentifier,
			@RequestParam("flowFilename") String flowFilename,
			@RequestParam("flowRelativePath") String flowRelativePath,
			@RequestParam("flowTotalChunks") int flowTotalChunks, @RequestParam("enclosureId") String enclosureId) {
		if (uploadServices.chunkExists(flowChunkNumber, enclosureId, flowIdentifier)) {
			return true;
		} else {
			response.setStatus(400);
			return false;
		}
	}

	@PostMapping("/upload")
	@Operation(method = "POST", description = "Upload  ")
	public void processUpload(HttpServletResponse response, @RequestParam("flowChunkNumber") int flowChunkNumber,
			@RequestParam("flowTotalChunks") int flowTotalChunks, @RequestParam("flowChunkSize") long flowChunkSize,
			@RequestParam("flowTotalSize") long flowTotalSize, @RequestParam("flowIdentifier") String flowIdentifier,
			@RequestParam("flowFilename") String flowFilename, @RequestParam("file") MultipartFile file,
			@RequestParam("enclosureId") String enclosureId, @RequestParam("senderId") String senderId,
			@RequestParam("senderToken") String senderToken) throws MetaloadException, StorageException {

		uploadServices.processPrivateUpload(flowChunkNumber, flowTotalChunks, flowIdentifier, file, enclosureId,
				senderId, senderToken);
		response.setStatus(HttpStatus.OK.value());

	}

	@PostMapping("/sender-info")
	@Operation(method = "POST", description = "sender Info ")
	public EnclosureRepresentation senderInfo(HttpServletRequest request, HttpServletResponse response,
			@RequestBody FranceTransfertDataRepresentation metadata) {
		LOGGER.info("start upload enclosure ");
		String token = metadata.getSenderToken();
		metadata.setConfirmedSenderId(metadata.getSenderId());
		EnclosureRepresentation enclosureRepresentation = uploadServices.senderInfoWithTockenValidation(metadata,
				token);
		response.setStatus(HttpStatus.OK.value());
		return enclosureRepresentation;
	}

	@PostMapping("/sender-contact")
	@Operation(method = "POST", description = "sender contact")
	public boolean senderContact(HttpServletRequest request, HttpServletResponse response,
			@Valid @RequestBody FormulaireContactData metadata) {
		LOGGER.info("start sending message ");
		boolean formulaire = uploadServices.senderContact(metadata);
		response.setStatus(HttpStatus.OK.value());
		return formulaire;
	}

	@PostMapping("/delete-file")
	@Operation(method = "GET", description = "Generate delete URL ")
	public EnclosureRepresentation deleteFile(HttpServletResponse response, @RequestBody DeleteRequest deleteRequest)
			throws MetaloadException {
		LOGGER.info("start delete file {}", deleteRequest.getEnclosureId());
		confirmationServices.validateAdminToken(deleteRequest.getEnclosureId(), deleteRequest.getToken(),
				deleteRequest.getSenderMail());
		uploadServices.validateExpirationDate(deleteRequest.getEnclosureId());
		EnclosureRepresentation enclosureRepresentation = uploadServices
				.updateExpiredTimeStamp(deleteRequest.getEnclosureId(), LocalDate.now().minusDays(1));
		return enclosureRepresentation;
	}

	@PostMapping("/update-expired-date")
	@Operation(method = "POST", description = "Update expired date")
	public ResponseEntity<Object> updateTimeStamp(HttpServletResponse response,
			@RequestBody @Valid DateUpdateRequest dateUpdateRequest)
			throws UnauthorizedAccessException, MetaloadException {
		confirmationServices.validateAdminToken(dateUpdateRequest.getEnclosureId(), dateUpdateRequest.getToken(),
				dateUpdateRequest.getSenderMail());

		uploadServices.validateExpirationDate(dateUpdateRequest.getEnclosureId());
		EnclosureRepresentation enclosureRepresentation = uploadServices
				.updateExpiredTimeStamp(dateUpdateRequest.getEnclosureId(), dateUpdateRequest.getNewDate());
		return new ResponseEntity<Object>(enclosureRepresentation, new HttpHeaders(), HttpStatus.OK);
	}

	@GetMapping("/validate-token")
	public ResponseEntity<Object> validateToken(
			@RequestParam @NotBlank(message = "Token must not be null") String token,
			@RequestParam @NotBlank(message = "EnclosureId must not be null") String enclosureId) {
		confirmationServices.validateAdminToken(enclosureId, token, null);
		return new ResponseEntity<Object>(true, new HttpHeaders(), HttpStatus.OK);
	}

	@PostMapping("/validate-code")
	@Operation(method = "POST", description = "Validate code")
	public EnclosureRepresentation validateCode(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("senderMail") String senderMail, @RequestParam("code") String code,
			@Valid @EmailsFranceTransfert @RequestBody FranceTransfertDataRepresentation metadata) {
		EnclosureRepresentation enclosureRepresentation = null;
		LOGGER.info("start validate confirmation code : " + code);
		code = code.trim();
		String cookieTocken = confirmationServices
				.validateCodeConfirmationAndGenerateToken(metadata.getSenderEmail().toLowerCase(), code)
				.getSenderToken();
		metadata.setConfirmedSenderId(metadata.getSenderId());
		enclosureRepresentation = uploadServices.senderInfoWithTockenValidation(metadata, cookieTocken);
		enclosureRepresentation.setSenderToken(cookieTocken);
		response.setStatus(HttpStatus.OK.value());
		return enclosureRepresentation;
	}

	// ---

	@GetMapping("/file-info")
	@Operation(method = "GET", description = "Download Info without URL ")
	public FileInfoRepresentation fileInfo(HttpServletResponse response, @RequestParam("enclosure") String enclosureId,
			@RequestParam("token") String token) throws UnauthorizedAccessException, MetaloadException {
		confirmationServices.validateAdminToken(enclosureId, token, null);
		FileInfoRepresentation fileInfoRepresentation = uploadServices.getInfoPlis(enclosureId);
		response.setStatus(HttpStatus.OK.value());
		return fileInfoRepresentation;
	}

	@PostMapping("/file-info-connect")
	@Operation(method = "POST", description = "Download Info without URL ")
	public FileInfoRepresentation fileInfoConnect(HttpServletResponse response,
			@RequestParam("enclosure") String enclosureId, @RequestBody ValidateCodeResponse metadata)
			throws UnauthorizedAccessException, MetaloadException {
		confirmationServices.validateAdminToken(enclosureId, metadata.getSenderToken(),
				metadata.getSenderMail());
		// add validate token service b body
		LOGGER.debug("-----------file-info connect-------- : {}", enclosureId);
		FileInfoRepresentation fileInfoRepresentation = uploadServices.getInfoPlis(enclosureId);
		response.setStatus(HttpStatus.OK.value());
		return fileInfoRepresentation;
	}

	@GetMapping("/file-info-reciever")
	@Operation(method = "GET", description = "Download Info without URL ")
	public FileInfoRepresentation fileInfoReciever(HttpServletResponse response,
			@RequestParam("enclosure") String enclosureId, @RequestParam("token") String token,
			@RequestParam("senderMail") String senderMail) throws UnauthorizedAccessException, MetaloadException {
		confirmationServices.validateToken(senderMail.toLowerCase(), token);
		FileInfoRepresentation fileInfoRepresentation = uploadServices.getInfoPlisForReciever(enclosureId, senderMail);
		response.setStatus(HttpStatus.OK.value());
		return fileInfoRepresentation;
	}

	@PostMapping("/get-plis-sent")
	@Operation(method = "POST", description = "Download Info without URL ")
	public List<FileInfoRepresentation> getPlisSent(HttpServletResponse response,
			@RequestBody ValidateCodeResponse metadata) throws UnauthorizedAccessException, MetaloadException {
		confirmationServices.validateToken(metadata.getSenderMail().toLowerCase(), metadata.getSenderToken());
		List<FileInfoRepresentation> listPlis = uploadServices.getSenderPlisList(metadata);
		response.setStatus(HttpStatus.OK.value());
		return listPlis;
	}

	@PostMapping("/get-plis-received")
	@Operation(method = "POST", description = "Download Info without URL ")
	public List<FileInfoRepresentation> getPliReceived(HttpServletResponse response,
			@RequestBody ValidateCodeResponse metadata) throws UnauthorizedAccessException, MetaloadException {
		confirmationServices.validateToken(metadata.getSenderMail().toLowerCase(), metadata.getSenderToken());

		List<FileInfoRepresentation> listPlis = uploadServices.getReceivedPlisList(metadata);

		response.setStatus(HttpStatus.OK.value());
		return listPlis;
	}

	@PostMapping("/add-recipient")
	@Operation(method = "POST", description = "add a new recipient")
	public boolean addRecipient(HttpServletResponse response,
			@RequestBody AddNewRecipientRequest addNewRecipientRequest)
			throws UnauthorizedAccessException, MetaloadException {
		confirmationServices.validateAdminToken(addNewRecipientRequest.getEnclosureId(),
				addNewRecipientRequest.getToken(), addNewRecipientRequest.getSenderMail());

		uploadServices.validateExpirationDate(addNewRecipientRequest.getEnclosureId());
		boolean res = uploadServices.addNewRecipientToMetaDataInRedis(addNewRecipientRequest.getEnclosureId(),
				addNewRecipientRequest.getNewRecipient());
		response.setStatus(HttpStatus.OK.value());
		return res;
	}

	@PostMapping("/delete-recipient")
	@Operation(method = "POST", description = "delete recipient")
	public boolean deleteRecipient(HttpServletResponse response,
			@RequestBody AddNewRecipientRequest addNewRecipientRequest)
			throws UnauthorizedAccessException, MetaloadException {
		confirmationServices.validateAdminToken(addNewRecipientRequest.getEnclosureId(),
				addNewRecipientRequest.getToken(), addNewRecipientRequest.getSenderMail());

		uploadServices.validateExpirationDate(addNewRecipientRequest.getEnclosureId());
		boolean res = uploadServices.logicDeleteRecipient(addNewRecipientRequest.getEnclosureId(),
				addNewRecipientRequest.getNewRecipient());
		response.setStatus(HttpStatus.OK.value());
		return res;
	}

	@PostMapping("/resend-download-link")
	@Operation(method = "POST", description = "add a new recipient")
	public boolean resendDownloadLink(HttpServletResponse response,
			@RequestBody AddNewRecipientRequest addNewRecipientRequest)
			throws UnauthorizedAccessException, MetaloadException {
		confirmationServices.validateAdminToken(addNewRecipientRequest.getEnclosureId(),
				addNewRecipientRequest.getToken(), addNewRecipientRequest.getSenderMail());

		uploadServices.validateExpirationDate(addNewRecipientRequest.getEnclosureId());
		boolean res = uploadServices.resendDonwloadLink(addNewRecipientRequest.getEnclosureId(),
				addNewRecipientRequest.getNewRecipient());
		response.setStatus(HttpStatus.OK.value());
		return res;
	}
	
	
	@RequestMapping(value = "/satisfaction", method = RequestMethod.POST)
	@Operation(method = "POST", description = "Rates the app on a scvale of 1 to 4")
	public boolean createSatisfactionFT(HttpServletResponse response,
			@Valid @RequestBody RateRepresentation rateRepresentation) throws UploadException {
		return rateServices.createSatisfactionFT(rateRepresentation);
	}

	@RequestMapping(value = "/validate-mail", method = RequestMethod.GET)
	@Operation(method = "GET", description = "Validate mail")
	public Boolean validateMailDomain(@RequestParam("mail") String mail) throws UploadException {
		ArrayList<String> list = new ArrayList<String>();
		list.add(mail);
		return uploadServices.validateMailDomain(list);
	}

	@RequestMapping(value = "/validate-mail", method = RequestMethod.POST)
	@Operation(method = "POST", description = "Validate mail")
	public Boolean validateMailDomain(@RequestBody List<String> mails) throws UploadException {
		return uploadServices.validateMailDomain(mails);
	}

	@RequestMapping(value = "/allowed-sender-mail", method = RequestMethod.POST)
	@Operation(method = "POST", description = "allowed sender mail")
	public Boolean allowedSenderMail(@RequestBody String mail) throws UploadException {
		return uploadServices.allowedSendermail(mail.toLowerCase());
	}

	@RequestMapping(value = "/config", method = RequestMethod.GET)
	@Operation(method = "GET", description = "Get Config")
	public ConfigRepresentation getConfig() {
		return configService.getConfig();
	}

	@RequestMapping(value = "/logout", method = RequestMethod.POST)
	@Operation(method = "POST", description = "Logout")
	public boolean logout(@RequestBody ValidateCodeResponse data) throws MetaloadException {
		return uploadServices.logout(data);
	}

}

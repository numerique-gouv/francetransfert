/* 
 * Copyright (c) Ministère de la Culture (2022) 
 * 
 * SPDX-License-Identifier: Apache-2.0 
 * License-Filename: LICENSE.txt 
 */

/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.upload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fr.gouv.culture.francetransfert.application.error.UnauthorizedAccessException;
import fr.gouv.culture.francetransfert.application.resources.model.FileRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.FilesInfosApi;
import fr.gouv.culture.francetransfert.application.resources.model.InitialisationInfo;
import fr.gouv.culture.francetransfert.application.resources.model.PackageInfoRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.PliInfoRepresentation;
import fr.gouv.culture.francetransfert.application.resources.model.StatusInfo;
import fr.gouv.culture.francetransfert.application.resources.model.ValidateCanal;
import fr.gouv.culture.francetransfert.application.resources.model.ValidateData;
import fr.gouv.culture.francetransfert.application.resources.model.ValidateUpload;
import fr.gouv.culture.francetransfert.application.services.UploadServices;
import fr.gouv.culture.francetransfert.application.services.ValidationMailService;
import fr.gouv.culture.francetransfert.core.exception.ApiValidationException;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.exception.StatException;
import fr.gouv.culture.francetransfert.core.exception.StorageException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@CrossOrigin
@RestController
@RequestMapping("/api-public")
@Tag(name = "Public resources")
@Validated
public class PublicResources {

	@Autowired
	private ValidationMailService validationMailService;
	@Autowired
	private UploadServices uploadServices;

	private static final String KEY = "cleAPI";
	private static final String FOR = "X-FORWARDED-FOR";
	private static final String TOKEN = "token";

	@PostMapping("/initPli")
	@Operation(method = "POST", description = "initPli")
	public InitialisationInfo validateCode(HttpServletResponse response, HttpServletRequest request,
			@Valid @RequestBody ValidateData metadata)
			throws ApiValidationException, MetaloadException, StorageException {

		String headerAddr = request.getHeader(KEY);
		String remoteAddr = request.getHeader(FOR);
		if (remoteAddr == null || "".equals(remoteAddr)) {
			remoteAddr = request.getRemoteAddr();
		}
		response.setStatus(201);
		return validationMailService.validateMailData(metadata, headerAddr, remoteAddr);
	}

	// ---
	@PostMapping("/chargementPli")
	@Operation(method = "POST", description = "chargementPli")
	public InitialisationInfo uploadData(HttpServletResponse response, HttpServletRequest request,
			@RequestParam(value = "numMorceauFichier", required = false) Integer flowChunkNumber,
			@RequestParam(value = "totalMorceauxFichier", required = false) Integer flowTotalChunks,
			@RequestParam(value = "tailleMorceauFichier", required = false) Long flowChunkSize,
			@RequestParam(value = "tailleFichier", required = false) Long flowTotalSize,
			@RequestParam("idFichier") String flowIdentifier, @RequestParam("nomFichier") String flowFilename,
			@RequestParam("fichier") MultipartFile file, @RequestParam("idPli") String enclosureId,
			@RequestParam("courrielExpediteur") String senderId)
			throws ApiValidationException, MetaloadException, StorageException {

		ValidateUpload metadata = new ValidateUpload();
		FileRepresentation rootFile = new FileRepresentation();

		rootFile.setFid(flowIdentifier);
		rootFile.setSize(flowTotalSize);
		rootFile.setName(flowFilename);

		metadata.setEnclosureId(enclosureId);
		metadata.setFlowChunkNumber(flowChunkNumber);
		metadata.setSenderEmail(senderId);
		metadata.setFlowChunkSize(flowChunkSize);
		metadata.setFlowTotalChunks(flowTotalChunks);
		metadata.setFichier(file);
		metadata.setRootFiles(rootFile);

		String headerAddr = request.getHeader(KEY);
		String remoteAddr = request.getHeader(FOR);
		String token = request.getHeader(TOKEN);
		if (remoteAddr == null || "".equals(remoteAddr)) {
			remoteAddr = request.getRemoteAddr();
		}
		response.setStatus(201);
		return validationMailService.validateUpload(metadata, headerAddr, remoteAddr, senderId, flowIdentifier, token);
	}

	@GetMapping("/statutPli")
	@Operation(method = "GET", description = "statutPli")
	public InitialisationInfo packageStatus(HttpServletResponse response, HttpServletRequest request,
			@RequestParam(value = "idPli", required = false) String enclosureId,
			@RequestParam(value = "courrielExpediteur", required = false) String senderMail)
			throws ApiValidationException, MetaloadException {

		String headerAddr = request.getHeader(KEY);
		String remoteAddr = request.getHeader(FOR);
		if (remoteAddr == null || "".equals(remoteAddr)) {
			remoteAddr = request.getRemoteAddr();
		}
		StatusInfo metadata = new StatusInfo(enclosureId, senderMail);
		return validationMailService.getStatusPli(metadata, headerAddr, remoteAddr);

	}

	@GetMapping("/donneesPli")
	@Operation(method = "GET", description = "donneesPli")
	public PackageInfoRepresentation packageInfo(HttpServletResponse response, HttpServletRequest request,
			@RequestParam(value = "idPli", required = false) String enclosureId,
			@RequestParam(value = "courrielExpediteur", required = false) String senderMail)
			throws ApiValidationException, MetaloadException, StatException {

		String headerAddr = request.getHeader(KEY);
		String remoteAddr = request.getHeader(FOR);
		if (remoteAddr == null || "".equals(remoteAddr)) {
			remoteAddr = request.getRemoteAddr();
		}

		StatusInfo metadata = new StatusInfo();
		metadata.setEnclosureId(enclosureId);
		metadata.setSenderMail(senderMail);
		return validationMailService.getInfoPli(metadata, headerAddr, remoteAddr);

	}

	@GetMapping("/mesPlis")
	@Operation(method = "GET", description = "get-plis")
	public FilesInfosApi getPlis(HttpServletResponse response, HttpServletRequest request,
			@RequestParam(value = "courrielUtilisateur", required = true) String courrielUtilisateur)
			throws UnauthorizedAccessException, MetaloadException, ApiValidationException {

		String headerAddr = request.getHeader(KEY);
		String remoteAddr = request.getHeader(FOR);
		if (remoteAddr == null || "".equals(remoteAddr)) {
			remoteAddr = request.getRemoteAddr();
		}

		return validationMailService.getPlisList(courrielUtilisateur, headerAddr, remoteAddr);
	}

	@GetMapping("/donneesPliDest")
	@Operation(method = "GET", description = "Get Info")
	public PliInfoRepresentation pliInfoConnectReciever(HttpServletResponse response, HttpServletRequest request,
			@RequestParam("idPli") String idPli, @RequestParam("courrielExpediteur") String courrielExpediteur,
			@RequestParam("courrielUtilisateur") String courrielUtilisateur)
			throws UnauthorizedAccessException, MetaloadException, ApiValidationException, StatException {

		String headerAddr = request.getHeader(KEY);
		String remoteAddr = request.getHeader(FOR);
		if (remoteAddr == null || "".equals(remoteAddr)) {
			remoteAddr = request.getRemoteAddr();
		}

		StatusInfo metadata = new StatusInfo();
		metadata.setEnclosureId(idPli);
		metadata.setSenderMail(courrielExpediteur);
		return validationMailService.getInfoPliReciever(metadata, courrielUtilisateur, headerAddr, remoteAddr);

	}

	@PostMapping("/majPreferenceDestinataire")
	@Operation(method = "POST", description = "majPreferenceDestinataire")
	public void majPreferenceDestinataire(HttpServletResponse response, HttpServletRequest request,
	        @RequestBody ValidateCanal metadata)
	        throws ApiValidationException, MetaloadException, StorageException {
	    
		String headerAddr = request.getHeader(KEY);
		String remoteAddr = request.getHeader(FOR);
		if (remoteAddr == null || "".equals(remoteAddr)) {
			remoteAddr = request.getRemoteAddr();
		}
		response.setStatus(201);
		validationMailService.majPreferenceDest(metadata, headerAddr, remoteAddr);
	}
}

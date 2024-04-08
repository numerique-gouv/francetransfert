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

package fr.gouv.culture.francetransfert.application.resources.download;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.gouv.culture.francetransfert.application.resources.model.Download;
import fr.gouv.culture.francetransfert.application.resources.model.DownloadPasswordMetaData;
import fr.gouv.culture.francetransfert.application.services.DownloadServices;
import fr.gouv.culture.francetransfert.core.exception.ApiValidationException;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.exception.StatException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@CrossOrigin
@RestController
@RequestMapping("/api-public-dl")
@Tag(name = "Public resources")
@Validated
public class PublicRessources {

	@Autowired
	DownloadServices downloadServices;

	private static final String KEY = "cleAPI";
	private static final String FOR = "X-FORWARDED-FOR";

	@PostMapping("/telechargerPli")
	@Operation(method = "POST", description = "Generate download URL ")
	public Download generateDownloadUrlWithPassword(HttpServletResponse response, HttpServletRequest request,
			@Valid @RequestBody DownloadPasswordMetaData downloadRequest)
			throws ApiValidationException, MetaloadException, StatException {

		String headerAddr = request.getHeader(KEY);
		String remoteAddr = request.getHeader(FOR);
		if (remoteAddr == null || "".equals(remoteAddr)) {
			remoteAddr = request.getRemoteAddr();
		}

		return downloadServices.getTelechargementPli(downloadRequest, headerAddr, remoteAddr);

	}

}

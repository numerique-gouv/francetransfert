/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import fr.gouv.culture.francetransfert.core.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecipientInfoApi {

	public RecipientInfoApi(RecipientInfo recInfo) {
		recipientMail = recInfo.getRecipientMail();
		numberOfDownloadPerRecipient = recInfo.getNumberOfDownloadPerRecipient();
		downloadDates = recInfo.getDownloadDates().stream().map(x -> {
			try {
				LocalDateTime parsedDate = LocalDateTime.parse(x);
				return DateUtils.convertLocalDateTimeToString(parsedDate, "yyyy-MM-dd'T'HH:mm:ss");
			} catch (Exception e) {
				return x;
			}
		}).collect(Collectors.toList());
	}

	@JsonProperty("courrielDestinataire")
	private String recipientMail;
	@JsonProperty("nbTelechargements")
	private int numberOfDownloadPerRecipient;
	@JsonProperty("datesTelechargement")
	private List<String> downloadDates;

}

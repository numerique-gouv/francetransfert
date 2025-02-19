/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import java.util.Map;

import com.nimbusds.oauth2.sdk.util.StringUtils;

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
public class TmpEnclosure implements Comparable<TmpEnclosure> {

	private String timestamp;
	private String enclosureId;
	private Map<String, String> meta;

	@Override
	public int compareTo(TmpEnclosure o) {
		if (o != null && StringUtils.isNotBlank(o.getTimestamp()) && timestamp != null
				&& StringUtils.isNotBlank(timestamp)) {
			return o.getTimestamp().compareTo(timestamp);
		}
		if (o != null && StringUtils.isNotBlank(o.getTimestamp())) {
			return -1;
		} else {
			return 1;
		}

	}

}

/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HealthCheckRepresentation {

	private boolean redis;
	private boolean s3;
	private boolean smtp;
	private boolean config;
	private String smtpUuid;
	private int smtpDelay;
	private int smtpPending;
	private boolean smtpDelayOk;
	private boolean glimps;

	public boolean isFtError() {
		if (redis == false || glimps == false || s3 == false || smtp == false || config == false
				|| smtpDelayOk == false) {
			return true;
		}
		return false;
	}

}

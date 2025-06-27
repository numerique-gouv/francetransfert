/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfigUpdate {

	private Map<String, String> messages;

}

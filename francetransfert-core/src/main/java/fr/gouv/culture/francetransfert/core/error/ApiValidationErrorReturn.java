/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.error;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Api validation error.
 * 
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiValidationErrorReturn {

	private List<ApiValidationError> erreurs;

}

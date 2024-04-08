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

/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.error;

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
public class ApiValidationError {

	/**
	 * libelleErreur ERROR
	 */
	private String libelleErreur;
	/**
	 * codeChamp ERROR
	 */
	private String codeChamp;
	/**
	 * numErreur ERROR
	 */
	private String numErreur;

}

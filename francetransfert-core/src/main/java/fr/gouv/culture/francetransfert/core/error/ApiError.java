/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.error;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * The type Api error.
 * 
 * @author Open Group
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
public class ApiError {
	/**
	 * Http Status Code
	 */
	private int statusCode;
	/**
	 * TYPE ERROR
	 */
	private String Type;
	/**
	 * ID ERROR
	 */
	private String id;
}

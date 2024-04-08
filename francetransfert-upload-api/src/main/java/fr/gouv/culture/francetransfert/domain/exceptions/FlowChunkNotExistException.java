/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.domain.exceptions;

public class FlowChunkNotExistException extends RuntimeException {
	public FlowChunkNotExistException(String flowIdentifier) {
		super(flowIdentifier);
	}
}

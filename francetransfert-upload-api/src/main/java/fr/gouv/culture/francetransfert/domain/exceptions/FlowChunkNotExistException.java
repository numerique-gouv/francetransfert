/*
  * Copyright (c) Direction Interministérielle du Numérique 
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

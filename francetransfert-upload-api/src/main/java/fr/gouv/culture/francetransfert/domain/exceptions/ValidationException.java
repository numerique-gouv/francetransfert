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

package fr.gouv.culture.francetransfert.domain.exceptions;

import lombok.Getter;

@Getter
public class ValidationException extends RuntimeException {
	private String codeChamp;
	private String numErreur;	
	private String libelleErreur;
	private Throwable ex;

	public ValidationException(String codeChamp, String numErreur, String libelleErreur) {
		super(libelleErreur);
		this.libelleErreur = libelleErreur;
		this.codeChamp = codeChamp;
		this.numErreur = numErreur;
	}

	public ValidationException(String libelleErreur) {
		super(libelleErreur);
		this.libelleErreur = libelleErreur;
	}

	public ValidationException(String libelleErreur, Throwable ex) {
		super(libelleErreur, ex);
		this.libelleErreur = libelleErreur;
	}

	public ValidationException(String libelleErreur, String codeChamp, String numErreur, Throwable ex) {
		super(libelleErreur, ex);
		this.libelleErreur = libelleErreur;
		this.codeChamp = codeChamp;
		this.numErreur = numErreur;
	}

	public ValidationException(Throwable ex) {
		super(ex);
		this.libelleErreur = ex.getMessage();
	}

}

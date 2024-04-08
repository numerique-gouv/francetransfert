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

package fr.gouv.culture.francetransfert.core.exception;

import java.util.List;

import fr.gouv.culture.francetransfert.core.error.ApiValidationError;
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
public class ApiValidationException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -7529278488199147655L;

    private List<ApiValidationError> erreurs;

    public ApiValidationException(List<ApiValidationError> erreurs, String message) {
        super(message);
        this.erreurs = erreurs;
    }

}

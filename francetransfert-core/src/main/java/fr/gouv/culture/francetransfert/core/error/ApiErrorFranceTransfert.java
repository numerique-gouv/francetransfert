/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ApiErrorFranceTransfert {
    private HttpStatus status;
    private String message;
    private Map<String,String> errors;

    public ApiErrorFranceTransfert(HttpStatus status, String message, Map<String,String> errors) {
        super();
        this.status = status;
        this.message = message;
        this.errors = errors;
    }
}

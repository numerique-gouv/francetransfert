/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.model.enums;

public enum ErrorEnum {
    TECHNICAL_ERROR("TECHNICAL_ERROR"),
    SCAN_ERROR("SCAN_ERROR"),
    LIMT_SIZE_ERROR("Clamd size limit exceeded"),
    PING_ERROR("Exception occurred while pinging clamav");

    private String value;

    ErrorEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

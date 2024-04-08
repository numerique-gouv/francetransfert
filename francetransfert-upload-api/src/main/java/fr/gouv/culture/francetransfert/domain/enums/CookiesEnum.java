/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.domain.enums;

public enum CookiesEnum {
    SENDER_ID("sender-id"),
    SENDER_TOKEN("sender-token"),
    IS_CONSENTED("HAS_CONSENTED");

    private String value;

    CookiesEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

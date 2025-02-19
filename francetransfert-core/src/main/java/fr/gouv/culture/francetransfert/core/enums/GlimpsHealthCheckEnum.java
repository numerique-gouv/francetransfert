/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.enums;

public enum GlimpsHealthCheckEnum {

    STATE_REAL("GlimpsStateReal"),STATE("GlimpsState"), SEND_AT("GlimpsSendAt");

    GlimpsHealthCheckEnum(String key) {
        this.setKey(key);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    private String key;

}

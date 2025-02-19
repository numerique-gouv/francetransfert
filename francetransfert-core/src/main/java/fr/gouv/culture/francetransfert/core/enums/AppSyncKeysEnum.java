/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.enums;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum AppSyncKeysEnum {
    APP_SYNC_CLEANUP("app-sync-cleanup"), APP_SYNC_RELAUNCH("app-sync-relaunch"),
    APP_SYNC_IGNIMISSION_DOMAIN("app-sync-ignimission-domain"), APP_SYNC_CHECK_MAIL_SEND("app-sync-check-mail-send"),
    APP_SYNC_CHECK_GLIMPS("app-sync-check-glimps"),
    APP_SYNC_CHECK_MAIL_CHECK("app-sync-check-mail-check");

    private String key;

    AppSyncKeysEnum(String key) {
        this.key = key;
    }

    public static List<String> keys() {
        return Stream.of(AppSyncKeysEnum.values()).map(e -> e.key).collect(Collectors.toList());
    }

    public String getKey() {
        return key;
    }
}

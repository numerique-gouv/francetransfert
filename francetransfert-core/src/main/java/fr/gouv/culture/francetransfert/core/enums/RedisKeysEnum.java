/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.enums;

public enum RedisKeysEnum {

    GENERIC("", ""), FT_ENCLOSURE_DATES("enclosure-dates:dates", ""),
    FT_ENCLOSURE_DATE("enclosure-date:", ":enclosures:ids"), FT_ENCLOSURE("enclosure:", ""),
    FT_SENDER("enclosure:", ":sender"), FT_RECIPIENTS("enclosure:", ":recipients:emails-ids"),
    FT_RECIPIENT("recipient:", ""), FT_RECIPIENT_ENCLOSURE("recipient:", "enclosure:"),
    FT_ROOT_FILES("enclosure:", ":contents:root-files:names"), FT_ADMIN_TOKEN("enclosure:", ":token-admin"),
    FT_ROOT_DIRS("enclosure:", ":contents:root-dirs:names"), FT_ROOT_FILE("root-file:", ""),
    FT_ROOT_DIR("root-dir:", ""), FT_FILES_IDS("enclosure:", ":contents:files:ids"), FT_FILE("file:", ""),
    FT_PART_ETAGS("file:", ":mul:part-etags"), FT_ID_CONTAINER("file:", ":mul:id-container"),
    FT_CODE_SENDER("sender:", ":confirmation-email-code"), FT_CODE_TRY("sender:", ":code-sender-try"),
    FT_TOKEN_SENDER("sender:", ":token"), FT_SENDER_PLIS("sender:", ":plis"),
    FT_DOMAINS_MAILS_MAILS("enclosure-mails:mails", ""),
    FT_DOMAINS_MAILS_RESANA("enclosure-mails:resana", ""), FT_DOMAINS_MAILS_OSMOSE("enclosure-mails:osmose", ""),
    FT_DOMAINS_MAILS_NOTIF_RIZOMO("enclosure-mails:rizomo-notif", ""),
    FT_DOMAINS_MAILS_TMP("enclosure-mails:tmp", ""),
    CHECK_MAIL("check-mail", ""), FT_SEND("send:", ""), FT_RECEIVE("receive:", ""),
    FT_Download_Date("recipient:", ":dates"), FT_ENCLOSURE_SCAN("enclosure:", ":scans"),
    FT_ENCLOSURE_VIRUS("enclosure:", ":virus"), FT_ENCLOSURE_SCAN_DELAY("enclosure:", ":scans-delay"),
    FT_ENCLOSURE_SCAN_RETRY("enclosure:", ":scans-retry"),
    FT_TOKEN_INITAPI("enclosureId:", ":upload-token");

    private String firstKeyPart;
    private String lastKeyPart;

    public String getFirstKeyPart() {
        return firstKeyPart;
    }

    public void setFirstKeyPart(String firstKeyPart) {
        this.firstKeyPart = firstKeyPart;
    }

    public String getLastKeyPart() {
        return lastKeyPart;
    }

    public void setLastKeyPart(String lastKeyPart) {
        this.lastKeyPart = lastKeyPart;
    }

    RedisKeysEnum(String firstKeyPart, String lastKeyPart) {
        this.firstKeyPart = firstKeyPart;
        this.lastKeyPart = lastKeyPart;
    }

    public String getKey(String key) {
        return firstKeyPart + key + lastKeyPart;
    }
}

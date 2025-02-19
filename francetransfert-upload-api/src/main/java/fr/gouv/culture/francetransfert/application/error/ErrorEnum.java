/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorEnum {
    TECHNICAL_ERROR("TECHNICAL_ERROR"),
    FUNCTIONAL_ERROR("FUNCTIONAL_ERROR"),
    LIMT_SIZE_ERROR("LIMT_SIZE_ERROR"),
    FILE_NAME_TOO_LONG("FILE_NAME_TOO_LONG"),
    INVALID_TOKEN("INVALID_TOKEN"),
    CONFIRMATION_CODE_ERROR("CONFIRMATION_CODE_ERROR"),
	RECIPIENT_DOESNT_EXIST("RECIPIENT_DOESNT_EXIST"),
    SENDER_MAIL_INVALID("SENDER_MAIL_INVALID"),
    SENDER_SEND_LIMIT("SENDER_SEND_LIMIT"),
    RECIPIENT_MAIL_INVALID("RECIPIENT_MAIL_INVALID"),
    MESSAGE_LENGTH_LIMIT("MESSAGE_LENGTH_LIMIT");
    private String value;
}

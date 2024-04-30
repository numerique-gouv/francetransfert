/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.resources.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EnclosureRepresentation {
    private String enclosureId;
    private String senderId;
    private String senderToken;
    private String expireDate;
    private Boolean canUpload = Boolean.TRUE;
}

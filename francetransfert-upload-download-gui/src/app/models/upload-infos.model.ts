/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

export interface UploadInfosModel {
    canUpload: boolean;
    enclosureId: string;
    expireDate: string;
    senderId: string;
    senderToken?: string;
}

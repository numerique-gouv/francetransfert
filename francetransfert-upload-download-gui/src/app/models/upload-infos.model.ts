/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

export interface UploadInfosModel {
    canUpload: boolean;
    enclosureId: string;
    expireDate: string;
    senderId: string;
    senderToken?: string;
}

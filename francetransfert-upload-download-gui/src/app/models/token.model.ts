/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

export interface TokenModel {
  senderMail: string;
  senderToken?: string;
  fromSso?: boolean
}

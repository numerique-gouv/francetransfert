/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

export interface TokenModel {
  senderMail: string;
  senderToken?: string;
  fromSso?: boolean
}

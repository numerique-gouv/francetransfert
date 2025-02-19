/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

export class PliModel {

  dateEnvoi: string;
  type: string;
  objet: string;
  taille: any;
  typeSize: string;
  finValidite: string;
  destinataires?: Array<String>;
  enclosureId: string;
  expired: string;
  matTooltip: string;
  nombreDest: number;
  destDownload?: number;
  roots?: Array<String>;
}

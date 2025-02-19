/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

export class PliRecuModel {

  dateReception: string;
  objet: string;
  taille: any;
  finValidite: string;
  expediteur?: string;
  enclosureId: string;
  typeSize: string;
  expired: string;
  matTooltip: string;
  roots?: Array<String>;
  downloadCount: number;
  firstDate?: Date;
  lastDate?: Date;


}

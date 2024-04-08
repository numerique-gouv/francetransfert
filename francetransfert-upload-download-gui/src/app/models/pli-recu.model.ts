/*
  * Copyright (c) Ministère de la Culture (2022)
  *
  * SPDX-License-Identifier: MIT
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

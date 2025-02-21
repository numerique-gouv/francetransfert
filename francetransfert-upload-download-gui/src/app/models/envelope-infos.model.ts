/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

export interface EnvelopeInfosModel {
    type?: 'mail' | 'link';
    from?: string;
    message?: string;
    subject?: string;
    cguCheck?: boolean;
    parameters?: ParametersModel;
}

export interface MailInfosModel extends EnvelopeInfosModel {
    subject?: string;
    to?: string[];
}

export interface LinkInfosModel extends EnvelopeInfosModel {
    subject?: string;
    to?: string[];
}

export interface ParametersModel {
    expiryDays: number;
    password: string;
    zipPassword: boolean;
    langueCourriels: any;
}

export interface FormulaireContactModel{
  nom?: string;
  prenom?: string;
  administration?: string;
  from?: string;
  message?: string;
  subject?: string;
}

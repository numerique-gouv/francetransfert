/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { DatePipe } from '@angular/common';
import { Provider } from '@angular/core';
import { DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE, MAT_NATIVE_DATE_FORMATS } from '@angular/material/core';
import { MAT_PAGINATOR_DEFAULT_OPTIONS, MatPaginatorIntl } from '@angular/material/paginator';
import Flow from '@flowjs/flow.js';
import { FlowInjectionToken } from '@flowjs/ngx-flow';

import { CustomPaginatorService } from './shared/custom-paginator/custom-paginator.service';
import { CustomDateAdapter } from './shared/custom-DateAdapter/custom-date-adapter';
import { FileMultipleSizePipe, FileNamePipe, FileSizePipe, FileTypePipe, TransfersMappingPipe } from './shared/pipes';
import { FileUnitPipe } from './shared/pipes/file-unit.pipe';


export const coreProviders: Provider[] = [
  { provide: MatPaginatorIntl, useClass: CustomPaginatorService },
  { provide: MAT_PAGINATOR_DEFAULT_OPTIONS, useValue: { formFieldAppearance: 'fill' } },
  { provide: MAT_DATE_LOCALE, useValue: 'fr-FR' },
  { provide: DateAdapter, useClass: CustomDateAdapter },
  { provide: MAT_DATE_FORMATS, useValue: MAT_NATIVE_DATE_FORMATS },
  DatePipe,
  FileMultipleSizePipe,
  FileNamePipe,
  FileSizePipe,
  FileTypePipe,
  TransfersMappingPipe,
  FileUnitPipe,
  { provide: FlowInjectionToken, useValue: Flow },
];

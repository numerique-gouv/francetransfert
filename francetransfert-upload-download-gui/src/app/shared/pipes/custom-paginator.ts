/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { MatPaginator, MatPaginatorIntl } from '@angular/material/paginator';

export class CustomPaginator extends MatPaginatorIntl {
  nextPageLabel: string;
  previousPageLabel: string;
  itemsPerPageLabel: string;
  constructor() {
    super();
    this.nextPageLabel = ' My new label for next page';
    this.previousPageLabel = ' My new label for previous page';
    this.itemsPerPageLabel = 'Task per screen';
  }
}

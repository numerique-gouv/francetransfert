/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { Transfer } from '@flowjs/ngx-flow';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { FTTransferModel } from 'src/app/models';
import { FileUnitPipe } from 'src/app/shared/pipes';
import { MatIcon } from '@angular/material/icon';
import { MatIconButton } from '@angular/material/button';
import { MatTooltip } from '@angular/material/tooltip';
import { FileNamePipe } from '../../../shared/pipes/file-name.pipe';
import { FileSizePipe } from '../../../shared/pipes/file-size.pipe';
import { FileTypePipe } from '../../../shared/pipes/file-type.pipe';

@Component({
    selector: 'ft-file-item',
    templateUrl: './file-item.component.html',
    styleUrls: ['./file-item.component.scss'],
    providers: [FileUnitPipe],
    imports: [MatIcon, MatIconButton, MatTooltip, TranslatePipe, FileNamePipe, FileSizePipe, FileTypePipe]
})
export class FileItemComponent implements OnInit {
  private translate = inject(TranslateService);
  private pipe = inject(FileUnitPipe);


  readonly transfer = input<FTTransferModel<Transfer>>(undefined);
  readonly readOnly = input<boolean>(false);
  readonly itemAdded = output<FTTransferModel<Transfer>>();
  readonly deletedTransfer = output<Transfer>();
  readonly unitSize = signal<string>(undefined);
  bytes: number;
  size: any;
  newUnit : any;
  constructor() {

  }

  ngOnInit(): void {
    this.itemAdded.emit(this.transfer());
    this.newUnit = this.pipe.transform(this.transfer().size);
    this.translate.stream(this.newUnit).subscribe(v => {
      this.unitSize.set(v);
    })


    }
  /**
   * Send transfer to delete
   * @returns {void}
   */
  deleteTransfer(): void {
      this.deletedTransfer.emit(this.transfer());
  }

}

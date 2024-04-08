/*
  * Copyright (c) Ministère de la Culture (2022)
  *
  * SPDX-License-Identifier: MIT
  * License-Filename: LICENSE.txt
  */

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Transfer } from '@flowjs/ngx-flow';
import { TranslateService } from '@ngx-translate/core';
import { FTTransferModel } from 'src/app/models';
import { FileUnitPipe } from 'src/app/shared/pipes';

@Component({
  selector: 'ft-file-item',
  templateUrl: './file-item.component.html',
  styleUrls: ['./file-item.component.scss'],
  providers: [ FileUnitPipe ]
})
export class FileItemComponent implements OnInit {

  @Input() transfer: FTTransferModel<Transfer>;
  @Input() readOnly: boolean = false;
  @Output() itemAdded: EventEmitter<FTTransferModel<Transfer>> = new EventEmitter();
  @Output() deletedTransfer: EventEmitter<Transfer>;
  unitSize: string;
  bytes: number;
  size: any;
  newUnit : any;
  constructor(
    private translate: TranslateService,
    private pipe: FileUnitPipe,
  ) {
    this.deletedTransfer = new EventEmitter();

  }

  ngOnInit(): void {
    this.itemAdded.emit(this.transfer);
    this.newUnit = this.pipe.transform(this.transfer.size);
    this.translate.stream(this.newUnit).subscribe(v => {
      this.unitSize = v;
    })


    }
  /**
   * Send transfer to delete
   * @returns {void}
   */
  deleteTransfer(): void {
      this.deletedTransfer.emit(this.transfer);
  }

}

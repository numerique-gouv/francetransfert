/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { Component, HostListener, OnInit, inject } from '@angular/core';
import { MatDialogRef, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose } from '@angular/material/dialog';
import { CdkScrollable } from '@angular/cdk/scrolling';
import { MatButton } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';
@Component({
    selector: 'ft-confirm-alert-dialog',
    templateUrl: './confirm-alert-dialog.component.html',
    styleUrls: ['./confirm-alert-dialog.component.scss'],
    imports: [MatDialogTitle, CdkScrollable, MatDialogContent, MatDialogActions, MatButton, MatDialogClose, TranslatePipe]
})
export class ConfirmAlertDialogComponent implements OnInit {
  private dialogRef = inject<MatDialogRef<ConfirmAlertDialogComponent>>(MatDialogRef);


  @HostListener('window:keyup', ['$event'])
  onDialogClick(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      this.close();
    }
  }

  isTrue: string = "false";

	close(): void {
		this.dialogRef.close(true);
	}

  ngOnInit(): void {
  }

  sendMessage(){
    this.isTrue = "true";
  }

}

/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: MIT 
  * License-Filename: LICENSE.txt 
  */

import { Component, HostListener, OnInit } from '@angular/core';
import { MatLegacyDialogRef as MatDialogRef } from '@angular/material/legacy-dialog';
@Component({
  selector: 'ft-confirm-alert-dialog',
  templateUrl: './confirm-alert-dialog.component.html',
  styleUrls: ['./confirm-alert-dialog.component.scss']
})
export class ConfirmAlertDialogComponent implements OnInit {

  @HostListener('window:keyup', ['$event'])
  onDialogClick(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      this.close();
    }
  }

  isTrue: string = "false";
  constructor(private dialogRef: MatDialogRef<ConfirmAlertDialogComponent>) { }

	close(): void {
		this.dialogRef.close(true);
	}

  ngOnInit(): void {
  }

  sendMessage(){
    this.isTrue = "true";
  }

}

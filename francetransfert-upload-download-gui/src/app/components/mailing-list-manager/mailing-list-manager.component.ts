/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, inject, signal } from '@angular/core';
import { MatDialogRef, MatDialogTitle, MatDialogContent, MatDialogActions } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MailingListService } from 'src/app/services';
import { DestinatairesEndMessageComponent } from '../destinataires-end-message/destinataires-end-message.component';
import { CdkScrollable } from '@angular/cdk/scrolling';
import { MatButton } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'ft-mailing-list-manager',
    templateUrl: './mailing-list-manager.component.html',
    styleUrls: ['./mailing-list-manager.component.scss'],
    imports: [MatDialogTitle, CdkScrollable, MatDialogContent, MatDialogActions, MatButton, TranslatePipe]
})
export class MailingListManagerComponent {
  private dialogRef = inject<MatDialogRef<MailingListManagerComponent>>(MatDialogRef);
  private mailingListService = inject(MailingListService);
  private _snackBar = inject(MatSnackBar);


  readonly errorMessage = signal('');

  loadMailingListFromLocalStorage() {
    this.dialogRef.close({ event: 'loadMailingListFromLocalStorage', data: this.mailingListService.getMailingList() });
  }

  loadMailingListFromFile(e) {
    const file = e.target.files[0];
    if (e.target.files[0].type.startsWith("text/") || (e.target.files[0].type == "application/vnd.ms-excel" && (e.target.files[0].name.endsWith(".csv") || e.target.files[0].name.endsWith(".txt")))) {
      let fileReader = new FileReader();
      fileReader.readAsText(file);
      fileReader.onload = (e) => {
        this.parseMailingListFile(fileReader.result);
      }
    }
    else {
      this.openSnackBar(4000);
    }

  }

  openSnackBar(duration: number) {
    this._snackBar.openFromComponent(DestinatairesEndMessageComponent, {
      duration: duration
    });
  }

  private parseMailingListFile(_data) {
    if (_data.includes(';')) {
      let splittedData = _data.replace(/\s/g, '').split(';');
      this.dialogRef.close({ event: 'loadMailingListFromFile', data: splittedData });
    } else {
      this.errorMessage.set('Le fichier importé est invalide.');
    }
  }

}

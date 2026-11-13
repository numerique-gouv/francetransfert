/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { Component, computed, inject, input } from '@angular/core';
import { DownloadManagerService } from 'src/app/services/download-manager/download-manager.service';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';



@Component({
    selector: 'ft-download-error',
    templateUrl: './download-error.component.html',
    styleUrls: ['./download-error.component.scss'],
    imports: [MatCard, MatCardContent, MatIcon, TranslatePipe]
})
export class DownloadErrorComponent {
  private downloadManagerService = inject(DownloadManagerService);


  readonly downloadAvailable = input<boolean>(undefined);
  readonly errorMessage = computed(() => {
    const error = this.downloadManagerService.downloadError();
    if (error && error.message == 'DOWNLOAD_LIMIT') {
      return 'Nombre_Téléchargements_Dépassé';
    } else if (error && error.message == 'DELETED_ENCLOSURE') {
      return 'Existence_Pli';
    } else if (error && error.message == 'HASH_INVALID') {
      return 'Pli_Corrompu';
    } else {
      return 'Erreur_Récupération_Pli';
    }
  });

}

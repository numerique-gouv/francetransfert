/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { Component, OnInit, inject } from '@angular/core';
import { MatBottomSheetRef, MAT_BOTTOM_SHEET_DATA } from '@angular/material/bottom-sheet';
import { MatToolbar } from '@angular/material/toolbar';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'app-pwa-prompt-install',
    templateUrl: './pwa-prompt-install.component.html',
    styleUrls: ['./pwa-prompt-install.component.scss'],
    imports: [MatToolbar, MatButton, MatIconButton, MatIcon, TranslatePipe]
})
export class PwaPromptInstallComponent implements OnInit {
  data = inject<{
    mobileType: 'ios' | 'android';
    promptEvent?: any;
}>(MAT_BOTTOM_SHEET_DATA);
  private bottomSheetRef = inject<MatBottomSheetRef<PwaPromptInstallComponent>>(MatBottomSheetRef);


  ngOnInit(): void {
  }

  public installPwa(): void {
    this.data.promptEvent.prompt();
    this.close();
  }

  public close() {
    this.bottomSheetRef.dismiss();
  }

}

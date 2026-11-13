/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */


import { Component, OnDestroy, OnInit, inject, output } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Transfer } from '@flowjs/ngx-flow';
import { FTTransferModel, LinkInfosModel, MailInfosModel, ParametersModel } from 'src/app/models';
import { AdminService, FileManagerService, UploadManagerService } from 'src/app/services';
import { LoginService } from 'src/app/services/login/login.service';
import { majChar, minChar, numChar, sizeControl, specialChar } from 'src/app/shared/validators/forms-validator';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatTabGroup, MatTab, MatTabContent } from '@angular/material/tabs';
import { PlisEnvoyesComponent } from './plis-envoyes/plis-envoyes.component';
import { PlisRecusComponent } from './plis-recus/plis-recus.component';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'ft-plis',
    templateUrl: './plis.component.html',
    styleUrls: ['./plis.component.scss'],
    imports: [MatCard, MatCardContent, MatTabGroup, MatTab, MatTabContent, PlisEnvoyesComponent, PlisRecusComponent, TranslatePipe]
})
export class PlisComponent implements OnInit {
  private loginService = inject(LoginService);
  private titleService = inject(Title);


  //added by abir
  fileInfos: any;
  transfers: Array<any> = [];
  validUntilDate;
  maxDate = new Date();
  public selectedDate: Date = new Date();


  readonly uploadStarted = output<boolean>();
  selectedTab: string;

  showParameters: boolean = false;

  parametersFormValues: ParametersModel;

  ngOnInit(): void {
    this.titleService.setTitle('France transfert - Mes plis');
  }

  selectedTabIndex(){
    return this.loginService.currentTabIndex
  }

  onSelectedTabChange(event) {
    this.loginService.currentTabIndex = event.index
  }





}

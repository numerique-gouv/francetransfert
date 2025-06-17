/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */


import { ChangeDetectorRef, Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Transfer } from '@flowjs/ngx-flow';
import { BehaviorSubject, Subscription, take } from 'rxjs';
import { FTTransferModel, LinkInfosModel, MailInfosModel, ParametersModel } from 'src/app/models';
import { TokenModel } from 'src/app/models/token.model';
import { AdminService, FileManagerService, UploadManagerService } from 'src/app/services';
import { LoginService } from 'src/app/services/login/login.service';
import { majChar, minChar, numChar, sizeControl, specialChar } from 'src/app/shared/validators/forms-validator';

@Component({
  selector: 'ft-plis',
  templateUrl: './plis.component.html',
  styleUrls: ['./plis.component.scss']
})
export class PlisComponent implements OnInit {

  //added by abir
  tokenInfo: BehaviorSubject<TokenModel> = new BehaviorSubject<any>(null);
  fileInfos: any;
  transfers: Array<any> = [];
  validUntilDate;
  maxDate = new Date();
  public selectedDate: Date = new Date();


  @Output() uploadStarted: EventEmitter<boolean> = new EventEmitter();
  selectedTab: string;

  showParameters: boolean = false;

  parametersFormValues: ParametersModel;

  constructor(private loginService: LoginService,
    private titleService: Title,
    ) {
  }

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

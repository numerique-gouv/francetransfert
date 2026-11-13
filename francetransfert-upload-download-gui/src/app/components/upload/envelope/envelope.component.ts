/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, computed, effect, inject, output, signal, untracked } from '@angular/core';
import { LinkInfosModel, MailInfosModel, ParametersModel } from 'src/app/models';
import { FileManagerService, UploadManagerService, UploadService } from 'src/app/services';
import { LoginService } from 'src/app/services/login/login.service';
import { majChar, minChar, numChar, sizeControl, specialChar, noSpecial, isDayNumberValid } from 'src/app/shared/validators/forms-validator';
import { ConfigService } from '../../../services/config/config.service';
import { MatCard, MatCardContent, MatCardActions, MatCardHeader, MatCardTitle } from '@angular/material/card';
import { MatTabGroup, MatTab, MatTabContent, MatTabLabel } from '@angular/material/tabs';
import { EnvelopeMailFormComponent } from './envelope-mail-form/envelope-mail-form.component';
import { MatButton } from '@angular/material/button';
import { MatTooltip } from '@angular/material/tooltip';
import { MatIcon } from '@angular/material/icon';
import { MatSuffix } from '@angular/material/select';
import { EnvelopeLinkFormComponent } from './envelope-link-form/envelope-link-form.component';
import { EnvelopeParametersFormComponent } from './envelope-parameters-form/envelope-parameters-form.component';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'ft-envelope',
    templateUrl: './envelope.component.html',
    styleUrls: ['./envelope.component.scss'],
    imports: [MatCard, MatCardContent, MatTabGroup, MatTab, MatTabContent, EnvelopeMailFormComponent, MatTabLabel, MatButton, MatTooltip, MatIcon, MatSuffix, EnvelopeLinkFormComponent, MatCardActions, MatCardHeader, MatCardTitle, EnvelopeParametersFormComponent, TranslatePipe]
})
export class EnvelopeComponent {
  private fileManagerService = inject(FileManagerService);
  private uploadManagerService = inject(UploadManagerService);
  private loginService = inject(LoginService);
  private uploadService = inject(UploadService);
  private configService = inject(ConfigService);


  readonly uploadStarted = output<boolean>();
  readonly selectedTabIndex = signal(0);
  private readonly mailFormValid = signal(false);
  private readonly linkFormValid = signal(false);
  
  readonly canSend = computed(() => this.fileManagerService.hasFiles()
    && (this.selectedTabIndex() === 0 ? this.mailFormValid() : this.linkFormValid()));
  readonly showParameters = signal<boolean>(false);
  
  readonly mailFormValues = signal<MailInfosModel>({ type: 'mail', from: '' });
  readonly linkFormValues = signal<LinkInfosModel>({ type: 'link', from: '' });
  readonly parametersFormValues = signal<ParametersModel>(undefined);
  private sessionMail = '';

  constructor() {
    
    effect(() => {
      const senderMail = this.loginService.senderMail();
      untracked(() => {
        if (senderMail !== this.sessionMail) {
          this.sessionMail = senderMail;
          this.mailFormValues().from = senderMail;
          this.linkFormValues().from = senderMail;
        }
      });
    });


    effect(() => {
      const _infos = this.uploadManagerService.envelopeInfos();
      untracked(() => {
        if (_infos) {
          if (_infos.type === 'mail') {
            this.mailFormValues.set(_infos);
          }
          if (_infos.type === 'link') {
            this.linkFormValues.set(_infos);
          }
          this.parametersFormValues.set(_infos.parameters);
        }
      });
    });
  }

  onSelectedTabChange(event) {
    this.selectedTabIndex.set(event.index);
  }

  onMailFormGroupChangeEvent(event) {
    
    event.values.to = event.destinataires;
    this.mailFormValues.set(event.values);
    this.mailFormValid.set(event.isValid && this.selectedTabIndex() === 0);
  }

  onLinkFormGroupChangeEvent(event) {
    this.linkFormValues.set(event.values);
    this.linkFormValid.set(event.isValid && this.selectedTabIndex() === 1);
  }

  onParametersFormGroupChangeEvent(event) {
    this.parametersFormValues.set(event.values);
  }

  isParamFromValid() {

    let checkpassword = true;
    let checkdate = true;
    const parameters = this.parametersFormValues();
    if (parameters && parameters.password && parameters.password != '' && parameters.password != null && parameters.password != undefined) {
      checkpassword = minChar(parameters.password)
        && majChar(parameters.password)
        && specialChar(parameters.password)
        && numChar(parameters.password)
        && sizeControl(parameters.password)
        && !noSpecial(parameters.password)
    }
    if (parameters && parameters.expiryDays) {
      checkdate = isDayNumberValid(parameters.expiryDays, this.configService);
    }
    return checkpassword && checkdate;
  }


  triggerShowParameters() {

    this.showParameters.set(!this.showParameters());
    const parameters = this.parametersFormValues();
    if (parameters?.expiryDays) {
      if (!isDayNumberValid(parameters.expiryDays, this.configService)) {
        parameters.expiryDays = 30;
      }
    }
    
  }


  triggerShowParametersValider() {

    this.uploadService.setCheckValidation(true);
    this.showParameters.set(!this.showParameters());

  }

  startUpload() {
    this.uploadService.setCheckValidation(false);
    this.uploadStarted.emit(true);
  }
}

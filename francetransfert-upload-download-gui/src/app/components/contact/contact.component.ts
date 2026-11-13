/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, ElementRef, OnInit, Signal, effect, inject, output, untracked, viewChild, viewChildren } from '@angular/core';
import { formSignal } from '../../shared/forms/form-signal';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from "@angular/forms";
import { FormulaireContactModel } from "../../models/envelope-infos.model";
import { ContactService } from "../../services/contact/contact.service";
import { MatSnackBar } from "@angular/material/snack-bar";
import { ContactEndMessageComponent } from "../contact-end-message/contact-end-message.component";
import { Subscription, take } from "rxjs";
import { v4 as uuidv4 } from 'uuid';
import { environment } from "../../../environments/environment";
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { MatDivider } from '@angular/material/list';
import { MatButton } from '@angular/material/button';
import { MatTooltip } from '@angular/material/tooltip';
import { MatIcon } from '@angular/material/icon';

@Component({
    selector: 'ft-contact',
    templateUrl: './contact.component.html',
    styleUrls: ['./contact.component.scss'],
    imports: [ReactiveFormsModule, MatDivider, MatButton, MatTooltip, MatIcon, TranslatePipe]
})
export class ContactComponent implements OnInit {
  private fb = inject(FormBuilder);
  private contactService = inject(ContactService);
  private _snackBar = inject(MatSnackBar);
  translateService = inject(TranslateService);
  private _router = inject(Router);
  private titleService = inject(Title);

  formulaireContactForm: Signal<FormGroup>;
  formulaireContact: FormulaireContactModel;
  formulaireBody: any;
  contatactFormChangeSubscription: Subscription;
  canSend: boolean = false;
  isSend: boolean = false;
  uuid = uuidv4();
  captchaType: string = 'IMAGE';
  CaptchaAudio: string;
  CaptchaVisuel: string;
  ChangerCaptcha: string;
  readonly sidenavToggle = output();

  readonly nom = viewChild<ElementRef>('nom');
  readonly myInput = viewChildren<ElementRef>('myInput');

  constructor() {
    this.uuid = uuidv4();
    this.formulaireContactForm = formSignal(this.fb.group({
      nom: [this.formulaireContact?.nom],
      prenom: [this.formulaireContact?.prenom],
      from: [this.formulaireContact?.from, { validators: [Validators.required, Validators.email], updateOn: 'blur' }],
      administration: [this.formulaireContact?.administration],
      subject: [this.formulaireContact?.subject],
      message: [this.formulaireContact?.message, { validators: [Validators.required] }],
      userResponse: ['', [Validators.required]]
    }));

    effect(() => {
      const inputs = this.myInput();
      untracked(() => this.focusAudio(inputs));
    });
  }

  ngOnInit(): void {
    this.titleService.setTitle('France transfert - Contact');
    this.initForm();

  }

  ngAfterViewInit() {
    this.nom()?.nativeElement.focus();
  }

  goToExternalLink(link: string, newTab: boolean = false) {
    if (newTab) {
      window.open(link, '_blank');
    } else {
      window.location.href = link;
    }
  }


  goToLink(url) {
    this._router.navigate([url]);
    this.sidenavToggle.emit();
  }


  initForm() {
    this.contatactFormChangeSubscription = this.formulaireContactForm().valueChanges
      .subscribe(() => {
        this.checkSend();
      });

    this.CaptchaAudio = 'CaptchaAudio';
    this.CaptchaVisuel = 'CaptchaVisuel';
    this.ChangerCaptcha = 'ChangerCaptcha';

  }

  get f() { return this.formulaireContactForm().controls; }

  checkSend() {
    this.canSend = this.formulaireContactForm().valid;
  }

  send() {
    this.formulaireBody = {
      ...this.formulaireContactForm().controls.nom.value ? { nom: this.formulaireContactForm().controls.nom.value } : { nom: "" },
      ...this.formulaireContactForm().controls.prenom.value ? { prenom: this.formulaireContactForm().controls.prenom.value } : { prenom: "" },
      ...this.formulaireContactForm().controls.from.value ? { from: this.formulaireContactForm().controls.from.value } : { from: "" },
      ...this.formulaireContactForm().controls.administration.value ? { administration: this.formulaireContactForm().controls.administration.value } : { administration: "" },
      ...this.formulaireContactForm().controls.subject.value ? { subject: this.formulaireContactForm().controls.subject.value } : { subject: "" },
      ...this.formulaireContactForm().controls.message.value ? { message: this.formulaireContactForm().controls.message.value } : { message: "" },
      userResponse: this.formulaireContactForm().controls.userResponse.value,
      challengeId: this.uuid,
      captchaType: this.captchaType,
    }
    this.contactService.sendFormulaireContact(this.formulaireBody).pipe(take(1)).subscribe((result: any) => {
      if (result) {
        this.openSnackBar(4000);
        //this.isSend = true;
        this._router.navigate(['/upload']);
      }
      this.reset();
    }, (error) => {
      if (error.status === 400) {
        this.formulaireContactForm().controls.userResponse.setValue('');
        this.formulaireContactForm().controls.userResponse.setErrors({ 'wrong': true });
        this.reset();
      }

    });
  }

  private reset() {
    this.uuid = uuidv4();
    this.canSend = false;
  }

  openSnackBar(duration: number) {
    this._snackBar.openFromComponent(ContactEndMessageComponent, {
      duration: duration
    });
  }

  focusAudio(inputs: readonly ElementRef[]) {
    if (this.captchaType == 'SOUND') {
      inputs[0]?.nativeElement.focus();
    }
  }

  switchType() {
    this.captchaType = this.captchaType === 'IMAGE' ? 'SOUND' : 'IMAGE';
  }

  getImageUrl() {
    return `${environment.apis.captcha.url}${this.uuid}${this.captchaType === 'IMAGE' ? '.jpg' : '.wav'}`;
  }

  resetUuid() {
    this.uuid = uuidv4();
  }

  ngOnDestroy() {
    this.contatactFormChangeSubscription.unsubscribe();
  }

}

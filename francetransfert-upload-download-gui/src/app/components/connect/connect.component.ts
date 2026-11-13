/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, ElementRef, OnInit, Signal, effect, inject, viewChild, viewChildren, signal } from '@angular/core';
import { formSignal } from '../../shared/forms/form-signal';
import { ConnectEndMessageComponent } from './../connect-end-message/connect-end-message.component';
import { FormControl, FormGroup, Validators, ReactiveFormsModule } from "@angular/forms";
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { take } from 'rxjs';
import { LoginService } from 'src/app/services/login/login.service';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { ConfigService } from 'src/app/services/config/config.service';
import { Title } from '@angular/platform-browser';
import { MatFormField, MatLabel, MatError } from '@angular/material/select';
import { MatInput } from '@angular/material/input';
import { MatCardActions } from '@angular/material/card';
import { MatButton } from '@angular/material/button';


@Component({
    selector: 'ft-connect',
    templateUrl: './connect.component.html',
    styleUrls: ['./connect.component.scss'],
    imports: [ReactiveFormsModule, MatFormField, MatLabel, MatInput, MatError, MatCardActions, MatButton, TranslatePipe]
})
export class ConnectComponent implements OnInit {
  private configService = inject(ConfigService);
  private loginService = inject(LoginService);
  translateService = inject(TranslateService);
  private _snackBar = inject(MatSnackBar);
  private router = inject(Router);
  private titleService = inject(Title);



  readonly mailEnter = viewChild<ElementRef>('mailEnter');
  readonly codeEnter = viewChild<ElementRef>('codeEnter');

  readonly loginForm: Signal<FormGroup>;

  readonly codeSent = signal<boolean>(false);
  readonly visible = signal<boolean>(false);
  readonly error = signal(null);
  readonly codeField = viewChildren<ElementRef>('codeReceived');
  readonly isAgentConnect = this.configService.isAgentConnect;

  constructor() {

    this.loginForm = formSignal(new FormGroup({
      email: new FormControl('', [Validators.required, Validators.email]),
      code: new FormControl('', [Validators.required, Validators.minLength(8)]),
    }));

    // The code field only exists once the code is sent: give it the focus as soon as it is rendered.
    effect(() => this.codeField().at(0)?.nativeElement.focus());
  }

  ngOnInit(): void {
    this.titleService.setTitle('France transfert - Connexion');
  }


  // onMouseOver(): void {
  //   this.imgSrc = "assets/images/agentconnect-btn-hover.png";
  // }

  // onMouseOut(): void {
  //   this.imgSrc = "assets/images/agentconnect-btn.png";
  // }

  cancel(event) {
    this.loginService.tokenInfo.set(null);
    this.loginForm().reset();
    this.codeSent.set(false);
    this.visible.set(false);
    this.error.set(null);
    this.mailEnter().nativeElement.focus();
  }

  backToHome() {
    this.router.navigate(['/upload']);
  }



  get email() { return this.loginForm().get('email') }
  get code() { return this.loginForm().get('code') }
  get f() { return this.loginForm().controls; }
  get form() { return this.loginForm(); }


  sendCode(event) {
    event.preventDefault();
    this.codeSent.set(!this.codeSent());
    this.visible.set(!this.visible());
    this.loginService.generateCode(this.email.value, this.translateService.currentLang()).pipe(take(1)).subscribe();
  }


  validateCode(event) {
    if (this.loginForm().valid) {
      this.error.set(null);
      this.loginService.validateCode({
        code: this.code.value,
        senderMail: this.email.value
      },
        this.translateService.currentLang()).pipe(take(1)).subscribe(x => {
          this.router.navigate(['/upload']);
        }, err => {
          this.error.set(err.error);
        });
    }
  }

  openSnackBar(duration: number) {
    this._snackBar.openFromComponent(ConnectEndMessageComponent, {
      duration: duration
    });
  }

  login(event) {
    event.preventDefault();
    this.loginService.login();
  }

}


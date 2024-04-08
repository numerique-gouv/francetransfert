/*
  * Copyright (c) Ministère de la Culture (2022)
  *
  * SPDX-License-Identifier: MIT
  * License-Filename: LICENSE.txt
  */

import { ChangeDetectorRef, Component, ElementRef, OnInit, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { ConnectEndMessageComponent } from './../connect-end-message/connect-end-message.component';
import { FormControl, FormGroup, Validators } from "@angular/forms";
import { MatLegacySnackBar as MatSnackBar } from '@angular/material/legacy-snack-bar';
import { Router } from '@angular/router';
import { Subscription, take } from 'rxjs';
import { LoginService } from 'src/app/services/login/login.service';
import { TranslateService } from '@ngx-translate/core';
import { ConfigService } from 'src/app/services/config/config.service';
import { Title } from '@angular/platform-browser';


@Component({
  selector: 'ft-connect',
  templateUrl: './connect.component.html',
  styleUrls: ['./connect.component.scss']
})
export class ConnectComponent implements OnInit {


  @ViewChild('mailEnter') mailEnter: ElementRef;
  @ViewChild('codeEnter', { static: false }) codeEnter: ElementRef;

  loginForm

  codeSent: boolean = false;
  visible: boolean = false;
  error = null;
  @ViewChildren('codeReceived') codeField: QueryList<ElementRef>;
  isAgentConnect: boolean = false;
  agentConnectSub: Subscription = new Subscription();

  constructor(
    private configService: ConfigService,
    private loginService: LoginService,
    public translateService: TranslateService,
    private _snackBar: MatSnackBar,
    private router: Router,
    private changeDetectorRef: ChangeDetectorRef,
    private titleService: Title,
    ) {

    this.loginForm = new FormGroup({
      email: new FormControl('', [Validators.required, Validators.email]),
      code: new FormControl('', [Validators.required, Validators.minLength(8)]),
    });

    this.agentConnectSub = this.configService.isAgentConnect.subscribe(x => {
      this.isAgentConnect = x;
    });

  }

  ngOnInit(): void {
    this.titleService.setTitle('France transfert - Connexion');
  }

  ngOnDestroy() {
    this.agentConnectSub.unsubscribe();
  }


  // onMouseOver(): void {
  //   this.imgSrc = "assets/images/agentconnect-btn-hover.png";
  // }

  // onMouseOut(): void {
  //   this.imgSrc = "assets/images/agentconnect-btn.png";
  // }

  cancel(event) {
    this.loginService.tokenInfo.next(null);
    this.loginForm.reset();
    this.codeSent = false;
    this.visible = false;
    this.error = null;
    this.mailEnter.nativeElement.focus();
  }

  backToHome() {
    this.router.navigate(['/upload']);
  }



  get email() { return this.loginForm.get('email') }
  get code() { return this.loginForm.get('code') }
  get f() { return this.loginForm.controls; }
  get form() { return this.loginForm; }


  sendCode(event) {
    event.preventDefault();
    this.codeSent = !this.codeSent;
    this.visible = !this.visible;
    this.loginService.generateCode(this.email.value, this.translateService.currentLang).pipe(take(1)).subscribe();
    this.changeDetectorRef.detectChanges();
    this.codeField.first.nativeElement.focus();
  }


  validateCode(event) {
    if (this.loginForm.valid) {
      this.error = null;
      this.loginService.validateCode({
        code: this.code.value,
        senderMail: this.email.value
      },
        this.translateService.currentLang).pipe(take(1)).subscribe(x => {
          this.router.navigate(['/upload']);
        }, err => {
          this.error = err.error;
        });
    }
  }

  openSnackBar(duration: number) {
    this._snackBar.openFromComponent(ConnectEndMessageComponent, {
      duration: duration
    });
  }

  login() {
    this.loginService.login();
  }

}


/*
  * Copyright (c) Ministère de la Culture (2022)
  *
  * SPDX-License-Identifier: MIT
  * License-Filename: LICENSE.txt
  */

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'ft-end-message',
  templateUrl: './end-message.component.html',
  styleUrls: ['./end-message.component.scss']
})
export class EndMessageComponent implements OnInit {

  @Input() component: 'upload' | 'download';
  @Input() availabilityDate: Date
  @Input() uploadFailed: boolean = false;
  @Output() backToHomeEvent: EventEmitter<any> = new EventEmitter();
  @Input() publicLink: boolean = false;

  constructor(private _router: Router,
    private _translate: TranslateService,) { }

  ngOnInit(): void {
  }

  backToHome(from: string) {
    if (from === 'upload') {
      this.backToHomeEvent.emit();
    } else {
      this._router.navigate(['/upload']);
    }
  }

  get translate(): TranslateService {
    return this._translate;
  }

}

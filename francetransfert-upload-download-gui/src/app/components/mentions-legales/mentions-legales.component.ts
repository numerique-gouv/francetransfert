/*
  * Copyright (c) Ministère de la Culture (2022)
  *
  * SPDX-License-Identifier: MIT
  * License-Filename: LICENSE.txt
  */

import { AfterViewInit, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

@Component({
  selector: 'ft-mentions-legales',
  templateUrl: './mentions-legales.component.html',
  styleUrls: ['./mentions-legales.component.scss']
})
export class MentionsLegalesComponent implements OnInit, AfterViewInit {

  @ViewChild('mentionslegales') private mentionslegalesFragment: ElementRef;
  @ViewChild('editeur') private editeurFragment: ElementRef;
  @ViewChild('responsabledelapublication') private responsabledelapublicationFragment: ElementRef;
  @ViewChild('liens') private liensFragment: ElementRef;
  @ViewChild('developpement') private developpementFragment: ElementRef;
  @ViewChild('hebergement') private hebergementFragment: ElementRef;
  @ViewChild('traductions') private traductionsFragment: ElementRef;

  constructor(private titleService: Title,
    private router: Router) { }

  ngOnInit(): void {
    this.titleService.setTitle('France transfert - Mentions légales');
  }

  ngAfterViewInit(): void {
    const tree = this.router.parseUrl(this.router.url);
    if (tree.fragment) {
      this.scrollTo(tree.fragment);
    }
  }

  scrollTo(_anchor: string) {
    switch (_anchor) {
      case 'mentionslegales':
        this.mentionslegalesFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'editeur':
        this.editeurFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'responsabledelapublication':
        this.responsabledelapublicationFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'liens':
        this.liensFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'developpement':
        this.developpementFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'hebergement':
        this.hebergementFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
        case 'traductions':
          this.traductionsFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
    }
  }
}

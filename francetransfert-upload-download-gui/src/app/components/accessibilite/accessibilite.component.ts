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
  selector: 'ft-accessibilite',
  templateUrl: './accessibilite.component.html',
  styleUrls: ['./accessibilite.component.scss']
})
export class AccessibiliteComponent implements OnInit, AfterViewInit {

  @ViewChild('accessibilite') private accessibiliteFragment: ElementRef;
  @ViewChild('declarationaccessibilite') private declarationaccessibiliteFragment: ElementRef;
  @ViewChild('etatdeconformite') private etatdeconformiteFragment: ElementRef;
  @ViewChild('retourinformationetcontact') private retourinformationetcontactFragment: ElementRef;
  @ViewChild('voiederecours') private voiederecoursFragment: ElementRef;

  constructor(private titleService: Title,
    private router: Router) { }

  ngOnInit(): void {
    this.titleService.setTitle('France transfert - Accessibilité');
  }

  ngAfterViewInit(): void {
    const tree = this.router.parseUrl(this.router.url);
    if (tree.fragment) {
      this.scrollTo(tree.fragment);
    }
  }

  scrollTo(_anchor: string) {
    switch (_anchor) {
      case 'accessibilite':
        this.accessibiliteFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'declarationaccessibilite':
        this.declarationaccessibiliteFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'etatdeconformite':
        this.etatdeconformiteFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'retourinformationetcontact':
        this.retourinformationetcontactFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'voiederecours':
        this.voiederecoursFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
    }
  }

}

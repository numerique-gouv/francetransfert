/*
  * Copyright (c) Ministère de la Culture (2022)
  *
  * SPDX-License-Identifier: MIT
  * License-Filename: LICENSE.txt
  */

import { AfterViewInit, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { DomSanitizer, Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

@Component({
  selector: 'ft-cgu',
  templateUrl: './cgu.component.html',
  styleUrls: ['./cgu.component.scss']
})
export class CguComponent implements OnInit, AfterViewInit {

  @ViewChild('cgu') private cguFragment: ElementRef;
  @ViewChild('presentationduservice') private presentationduserviceFragment: ElementRef;
  @ViewChild('modalites') private modalitesFragment: ElementRef;
  @ViewChild('traitementdesdonnees') private traitementdesdonneesFragment: ElementRef;
  @ViewChild('conservationdesdonnees') private conservationdesdonneesFragment: ElementRef;
  @ViewChild('engagementsetresponsabilite') private engagementsetresponsabiliteFragment: ElementRef;
  @ViewChild('qualiteduservice') private qualiteduserviceFragment: ElementRef;
  @ViewChild('securite') private securiteFragment: ElementRef;
  @ViewChild('contact') private  contact: ElementRef;
  @ViewChild('validite') private validiteFragment: ElementRef;
  sanitizedUrl: any;

  constructor(private titleService: Title,
    private sanitizer:DomSanitizer,
    private router: Router) { }

  ngOnInit(): void {
    this.titleService.setTitle('France transfert - CGU');
    this.sanitizedUrl = this.sanitizer.bypassSecurityTrustUrl('https://www.ssi.gouv.fr/administration/qualification/zed/');
  }

  ngAfterViewInit(): void {
    const tree = this.router.parseUrl(this.router.url);
    if (tree.fragment) {
      this.scrollTo(tree.fragment);
    }
  }

  scrollTo(_anchor: string) {
    switch (_anchor) {
      case 'cgu':
        this.cguFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'presentationduservice':
        this.presentationduserviceFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'modalites':
        this.modalitesFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'traitementdesdonnees':
        this.traitementdesdonneesFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'conservationdesdonnees':
        this.conservationdesdonneesFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'engagementsetresponsabilite':
        this.engagementsetresponsabiliteFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'qualiteduservice':
        this.qualiteduserviceFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'securite':
        this.securiteFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'contact':
        this.contact.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'validite':
        this.validiteFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
    }
  }
}

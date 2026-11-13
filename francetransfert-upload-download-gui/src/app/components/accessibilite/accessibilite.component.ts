/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { AfterViewInit, Component, ElementRef, OnInit, inject, viewChild } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { MatCard, MatCardHeader, MatCardContent } from '@angular/material/card';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'ft-accessibilite',
    templateUrl: './accessibilite.component.html',
    styleUrls: ['./accessibilite.component.scss'],
    imports: [MatCard, MatCardHeader, MatCardContent, TranslatePipe]
})
export class AccessibiliteComponent implements OnInit, AfterViewInit {
  private titleService = inject(Title);
  private router = inject(Router);


  private readonly accessibiliteFragment = viewChild<ElementRef>('accessibilite');
  private readonly declarationaccessibiliteFragment = viewChild<ElementRef>('declarationaccessibilite');
  private readonly etatdeconformiteFragment = viewChild<ElementRef>('etatdeconformite');
  private readonly retourinformationetcontactFragment = viewChild<ElementRef>('retourinformationetcontact');
  private readonly voiederecoursFragment = viewChild<ElementRef>('voiederecours');

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
        this.accessibiliteFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'declarationaccessibilite':
        this.declarationaccessibiliteFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'etatdeconformite':
        this.etatdeconformiteFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'retourinformationetcontact':
        this.retourinformationetcontactFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'voiederecours':
        this.voiederecoursFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
    }
  }

}

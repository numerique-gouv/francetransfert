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
    selector: 'ft-mentions-legales',
    templateUrl: './mentions-legales.component.html',
    styleUrls: ['./mentions-legales.component.scss'],
    imports: [MatCard, MatCardHeader, MatCardContent, TranslatePipe]
})
export class MentionsLegalesComponent implements OnInit, AfterViewInit {
  private titleService = inject(Title);
  private router = inject(Router);


  private readonly mentionslegalesFragment = viewChild<ElementRef>('mentionslegales');
  private readonly editeurFragment = viewChild<ElementRef>('editeur');
  private readonly responsabledelapublicationFragment = viewChild<ElementRef>('responsabledelapublication');
  private readonly liensFragment = viewChild<ElementRef>('liens');
  private readonly developpementFragment = viewChild<ElementRef>('developpement');
  private readonly hebergementFragment = viewChild<ElementRef>('hebergement');
  private readonly traductionsFragment = viewChild<ElementRef>('traductions');

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
        this.mentionslegalesFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'editeur':
        this.editeurFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'responsabledelapublication':
        this.responsabledelapublicationFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'liens':
        this.liensFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'developpement':
        this.developpementFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'hebergement':
        this.hebergementFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
        case 'traductions':
          this.traductionsFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
    }
  }
}

/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { AfterViewInit, Component, ElementRef, OnInit, computed, inject, viewChild } from '@angular/core';
import { DomSanitizer, Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { ConfigService } from '../../services/config/config.service';
import { MatCard, MatCardHeader, MatCardContent } from '@angular/material/card';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'ft-cgu',
    templateUrl: './cgu.component.html',
    styleUrls: ['./cgu.component.scss'],
    imports: [MatCard, MatCardHeader, MatCardContent, TranslatePipe]
})
export class CguComponent implements OnInit, AfterViewInit {
  private titleService = inject(Title);
  private sanitizer = inject(DomSanitizer);
  private router = inject(Router);
  private configService = inject(ConfigService);


  private readonly cguFragment = viewChild<ElementRef>('cgu');
  private readonly presentationduserviceFragment = viewChild<ElementRef>('presentationduservice');
  private readonly modalitesFragment = viewChild<ElementRef>('modalites');
  private readonly traitementdesdonneesFragment = viewChild<ElementRef>('traitementdesdonnees');
  private readonly conservationdesdonneesFragment = viewChild<ElementRef>('conservationdesdonnees');
  private readonly engagementsetresponsabiliteFragment = viewChild<ElementRef>('engagementsetresponsabilite');
  private readonly qualiteduserviceFragment = viewChild<ElementRef>('qualiteduservice');
  private readonly securiteFragment = viewChild<ElementRef>('securite');
  private readonly contact = viewChild<ElementRef>('contact');
  private readonly validiteFragment = viewChild<ElementRef>('validite');
  sanitizedUrl: any;
  readonly jours = computed(() => this.configService.configInfo()?.uploadExpiredLimit);

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
        this.cguFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'presentationduservice':
        this.presentationduserviceFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'modalites':
        this.modalitesFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'traitementdesdonnees':
        this.traitementdesdonneesFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'conservationdesdonnees':
        this.conservationdesdonneesFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'engagementsetresponsabilite':
        this.engagementsetresponsabiliteFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'qualiteduservice':
        this.qualiteduserviceFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'securite':
        this.securiteFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'contact':
        this.contact().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'validite':
        this.validiteFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
    }
  }
}

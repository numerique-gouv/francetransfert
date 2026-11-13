/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { AfterViewInit, Component, ElementRef, OnInit, computed, inject, viewChild } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { ConfigService } from '../../services/config/config.service';
import { MatCard, MatCardHeader, MatCardContent } from '@angular/material/card';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'ft-politique-protection-donnees',
    templateUrl: './politique-protection-donnees.component.html',
    styleUrls: ['./politique-protection-donnees.component.scss'],
    imports: [MatCard, MatCardHeader, MatCardContent, TranslatePipe]
})
export class PolitiqueProtectionDonneesComponent implements OnInit, AfterViewInit {
  private titleService = inject(Title);
  private router = inject(Router);
  private configService = inject(ConfigService);


  private readonly politiquedeprotectiondesdonneesFragment = viewChild<ElementRef>('politiquedeprotectiondesdonnees');
  private readonly droitsdespersonnesFragment = viewChild<ElementRef>('droitsdespersonnes');
  readonly jours = computed(() => this.configService.configInfo()?.uploadExpiredLimit);

  ngOnInit(): void {
    this.titleService.setTitle('France transfert - Politique de protection des données');
  }

  ngAfterViewInit(): void {
    const tree = this.router.parseUrl(this.router.url);
    if (tree.fragment) {
      this.scrollTo(tree.fragment);
    }
  }

  scrollTo(_anchor: string) {
    switch (_anchor) {
      case 'politiquedeprotectiondesdonnees':
        this.politiquedeprotectiondesdonneesFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'droitsdespersonnes':
        this.droitsdespersonnesFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
    }
  }

}

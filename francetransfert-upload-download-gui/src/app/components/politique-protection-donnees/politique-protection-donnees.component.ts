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
  selector: 'ft-politique-protection-donnees',
  templateUrl: './politique-protection-donnees.component.html',
  styleUrls: ['./politique-protection-donnees.component.scss']
})
export class PolitiqueProtectionDonneesComponent implements OnInit, AfterViewInit {

  @ViewChild('politiquedeprotectiondesdonnees') private politiquedeprotectiondesdonneesFragment: ElementRef;
  @ViewChild('droitsdespersonnes') private droitsdespersonnesFragment: ElementRef;

  constructor(private titleService: Title,
    private router: Router) { }

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
        this.politiquedeprotectiondesdonneesFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'droitsdespersonnes':
        this.droitsdespersonnesFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
    }
  }

}

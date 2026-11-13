/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { AfterViewInit, Component, ElementRef, OnInit, ViewEncapsulation, inject, viewChild, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { TranslateService, LangChangeEvent, TranslatePipe } from '@ngx-translate/core';
import { map, Subscription } from 'rxjs';
import { LanguageModel, CategoryModel } from 'src/app/models';
import { LanguageSelectionService } from 'src/app/services';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { MatCard, MatCardHeader, MatCardContent } from '@angular/material/card';
import { MatFormField, MatPrefix } from '@angular/material/select';
import { MatInput } from '@angular/material/input';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { MatAccordion, MatExpansionPanel, MatExpansionPanelHeader, MatExpansionPanelTitle } from '@angular/material/expansion';
import { FilterQuestionsPipe } from '../../shared/pipes/FilterQuestionsPipe.pipe';



@Component({
    selector: 'ft-faq',
    templateUrl: './faq.component.html',
    styleUrls: ['./faq.component.scss'],
    encapsulation: ViewEncapsulation.None,
    imports: [MatCard, MatCardHeader, MatCardContent, MatFormField, MatPrefix, MatInput, ReactiveFormsModule, FormsModule, MatAccordion, MatExpansionPanel, MatExpansionPanelHeader, MatExpansionPanelTitle, TranslatePipe, FilterQuestionsPipe]
})


export class FaqComponent implements OnInit, AfterViewInit {
  private titleService = inject(Title);
  private router = inject(Router);
  private translateService = inject(TranslateService);
  private languageSelectionService = inject(LanguageSelectionService);
  private route = inject(ActivatedRoute);


  private readonly faqFragment = viewChild<ElementRef>('faq');
  private readonly pourquoiftFragment = viewChild<ElementRef>('pourquoift');
  private readonly usagepersonnelFragment = viewChild<ElementRef>('usagepersonnel');
  private readonly quedeviennentlesfichiersFragment = viewChild<ElementRef>('quedeviennentlesfichiers');
  private readonly ouvontlesfichiersFragment = viewChild<ElementRef>('ouvontlesfichiers');
  private readonly codedeconfirmationFragment = viewChild<ElementRef>('codedeconfirmation');
  private readonly limitedetailleFragment = viewChild<ElementRef>('limitedetaille');
  private readonly extensionsdefichiersFragment = viewChild<ElementRef>('extensionsdefichiers');
  private readonly listededistributionFragment = viewChild<ElementRef>('listededistribution');
  private readonly questionnairedesatisfactionFragment = viewChild<ElementRef>('questionnairedesatisfaction');
  private readonly traitementapreslenvoiedesfichiers = viewChild<ElementRef>('traitementapreslenvoiedesfichiers');
  private readonly tromperDansListDestinataires = viewChild<ElementRef>('tromperDansListDestinataires');
  private readonly nombreDeTelechargement = viewChild<ElementRef>('nombreDeTelechargement');
  private readonly ouTrouverDesInfoSurFT = viewChild<ElementRef>('ouTrouverDesInfoSurFT');
  private readonly utilisationsurmobile = viewChild<ElementRef>('utilisationsurmobile');
  private readonly deuxfaçondutiliserfrancetransfert = viewChild<ElementRef>('deuxfaçondutiliserfrancetransfert');



  languageList: LanguageModel[];
  languageSelectionSubscription: Subscription;
  currentLanguage: string;
  language: LanguageModel;
  langueCode: string;
  panelOpenState = false;


  readonly categories = signal<any>(undefined);

  queryString;
  readonly searchableList = signal<string[]>(undefined);
  searchableList0: string[];
  public currentLang: string;
  readonly stateI = signal<any>(undefined);
  stateJ: any;


  constructor() {

    this.getListQuetion();
    this.currentLanguage = this.translateService.currentLang();

    this.searchableList.set(['Question', 'Question_Texte1', 'Question_Liste', 'Question_Liste_Ordered', 'Question_Texte2', 'QuestionImage'])



  }
  ngOnInit(): void {
    this.titleService.setTitle('France transfert - FAQ');

  }


  getListQuetion() {

    this.translateService.stream("questions").subscribe(v => {
      this.categories.set(v)
    })

  }

  ngAfterViewInit(): void {
    const tree = this.router.parseUrl(this.router.url);
    if (tree.fragment) {
      this.scrollTo(tree.fragment);
    }
  }

  scrollTo(_anchor: string) {
    switch (_anchor) {
      case 'faq':
        this.faqFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'pourquoift':
        this.pourquoiftFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'usagepersonnel':
        this.usagepersonnelFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'deuxfaçondutiliserfrancetransfert':
        this.deuxfaçondutiliserfrancetransfert().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'utilisationsurmobile':
        this.utilisationsurmobile().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'codedeconfirmation':
        this.codedeconfirmationFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'traitementapreslenvoiedesfichiers':
        this.traitementapreslenvoiedesfichiers().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'tromperDansListDestinataires':
        this.tromperDansListDestinataires().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'nombreDeTelechargement':
        this.nombreDeTelechargement().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'quedeviennentlesfichiers':
        this.quedeviennentlesfichiersFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'ouvontlesfichiers':
        this.ouvontlesfichiersFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'limitedetaille':
        this.limitedetailleFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'extensionsdefichiers':
        this.extensionsdefichiersFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'listededistribution':
        this.listededistributionFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'questionnairedesatisfaction':
        this.questionnairedesatisfactionFragment().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'ouTrouverDesInfoSurFT':
        this.ouTrouverDesInfoSurFT().nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
    }
  }

  setState(i: number, j: number) {
    this.stateI.set(i);
    this.stateJ = j;
  }


}

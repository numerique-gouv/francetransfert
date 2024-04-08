/*
  * Copyright (c) Ministère de la Culture (2022)
  *
  * SPDX-License-Identifier: MIT
  * License-Filename: LICENSE.txt
  */

import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { TranslateService, LangChangeEvent } from '@ngx-translate/core';
import { BehaviorSubject, map, Subscription } from 'rxjs';
import { LanguageModel, CategoryModel } from 'src/app/models';
import { LanguageSelectionService } from 'src/app/services';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs/internal/Observable';



@Component({
  selector: 'ft-faq',
  templateUrl: './faq.component.html',
  styleUrls: ['./faq.component.scss'],
  encapsulation: ViewEncapsulation.None,



})


export class FaqComponent implements OnInit, AfterViewInit {

  @ViewChild('faq') private faqFragment: ElementRef;
  @ViewChild('pourquoift') private pourquoiftFragment: ElementRef;
  @ViewChild('usagepersonnel') private usagepersonnelFragment: ElementRef;
  @ViewChild('quedeviennentlesfichiers') private quedeviennentlesfichiersFragment: ElementRef;
  @ViewChild('ouvontlesfichiers') private ouvontlesfichiersFragment: ElementRef;
  @ViewChild('codedeconfirmation') private codedeconfirmationFragment: ElementRef;
  @ViewChild('limitedetaille') private limitedetailleFragment: ElementRef;
  @ViewChild('extensionsdefichiers') private extensionsdefichiersFragment: ElementRef;
  @ViewChild('listededistribution') private listededistributionFragment: ElementRef;
  @ViewChild('questionnairedesatisfaction') private questionnairedesatisfactionFragment: ElementRef;
  @ViewChild('traitementapreslenvoiedesfichiers') private traitementapreslenvoiedesfichiers: ElementRef;
  @ViewChild('tromperDansListDestinataires') private tromperDansListDestinataires: ElementRef;
  @ViewChild('nombreDeTelechargement') private nombreDeTelechargement: ElementRef;
  @ViewChild('ouTrouverDesInfoSurFT') private ouTrouverDesInfoSurFT: ElementRef;
  @ViewChild('utilisationsurmobile') private utilisationsurmobile: ElementRef;
  @ViewChild('deuxfaçondutiliserfrancetransfert') private deuxfaçondutiliserfrancetransfert: ElementRef;



  languageList: LanguageModel[];
  languageSelectionSubscription: Subscription;
  currentLanguage: string;
  language: LanguageModel;
  langueCode: string;
  panelOpenState = false;


  categories: any;

  queryString;
  searchableList: string[];
  searchableList0: string[];
  public currentLang: string;
  stateI: any;
  stateJ: any;


  constructor(private titleService: Title,
    private router: Router,
    private translateService: TranslateService,
    private languageSelectionService: LanguageSelectionService,
    private route: ActivatedRoute,
  ) {

    this.getListQuetion();
    this.currentLanguage = this.translateService.currentLang;

    this.searchableList = ['Question', 'Question_Texte1', 'Question_Liste', 'Question_Liste_Ordered', 'Question_Texte2', 'QuestionImage']



  }
  ngOnInit(): void {
    this.titleService.setTitle('France transfert - FAQ');

  }


  getListQuetion() {

    this.translateService.stream("questions").subscribe(v => {
      this.categories = v
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
        this.faqFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'pourquoift':
        this.pourquoiftFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'usagepersonnel':
        this.usagepersonnelFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'deuxfaçondutiliserfrancetransfert':
        this.deuxfaçondutiliserfrancetransfert.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'utilisationsurmobile':
        this.utilisationsurmobile.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'codedeconfirmation':
        this.codedeconfirmationFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'traitementapreslenvoiedesfichiers':
        this.traitementapreslenvoiedesfichiers.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'tromperDansListDestinataires':
        this.tromperDansListDestinataires.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'nombreDeTelechargement':
        this.nombreDeTelechargement.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'quedeviennentlesfichiers':
        this.quedeviennentlesfichiersFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'ouvontlesfichiers':
        this.ouvontlesfichiersFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'limitedetaille':
        this.limitedetailleFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'extensionsdefichiers':
        this.extensionsdefichiersFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'listededistribution':
        this.listededistributionFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'questionnairedesatisfaction':
        this.questionnairedesatisfactionFragment.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case 'ouTrouverDesInfoSurFT':
        this.ouTrouverDesInfoSurFT.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
    }
  }

  setState(i: number, j: number) {
    this.stateI = i;
    this.stateJ = j;
  }


}

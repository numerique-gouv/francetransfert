/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, ViewChild, AfterViewInit, OnInit } from '@angular/core';
import { MatLegacyPaginator as MatPaginator, MatLegacyPaginatorIntl as MatPaginatorIntl } from '@angular/material/legacy-paginator';
import { MatLegacyTableDataSource as MatTableDataSource } from '@angular/material/legacy-table';
import {LangChangeEvent, TranslateService} from '@ngx-translate/core';
import { AdminService, ResponsiveService } from 'src/app/services';
import { LoginService } from 'src/app/services/login/login.service';
import { take } from 'rxjs';
import { PliModel } from 'src/app/models/pli.model';
import { Router } from '@angular/router';
import { MatSort } from '@angular/material/sort';
import { Subscription } from 'rxjs/internal/Subscription';
import { FormControl } from '@angular/forms';
import { Transfer } from '@flowjs/ngx-flow';
import { FTTransferModel } from 'src/app/models';
import {MatPaginatorModule, PageEvent} from "@angular/material/paginator";
import {CustomPaginatorService} from "../../../shared/custom-paginator/custom-paginator.service";
import {LoaderService} from "../../../services/loader/loader.service";
import {DateAdapter} from "@angular/material/core";
import {MyDateAdapter, MyDefaultDateAdapter} from "../../upload/envelope/envelope-parameters-form/my-date-adapter";
import * as moment from "moment";


interface Type {
  value: string;
  viewValue: string;
}


@Component({
  selector: 'ft-plis-envoyes',
  templateUrl: './plis-envoyes.component.html',
  styleUrls: ['./plis-envoyes.component.scss'],
  providers: [{provide: MatPaginatorIntl, useClass: CustomPaginatorService},{provide: DateAdapter, useClass: MyDefaultDateAdapter}],
})
export class PlisEnvoyesComponent implements OnInit, AfterViewInit{

  //@ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatPaginator) paginator: MatPaginator;
  empList: PliModel[] = [];
  displayedColumns: string[] = ['dateEnvoi', 'type', 'objet', 'taille', 'finValidite', 'destinataires', 'expired'];
  dataSource = new MatTableDataSource<PliModel>(this.empList);
  subscriptions: Subscription[] = [];
  hideSubscription: Subscription= new Subscription;
  isMobile;
  screenWidth;

  destinatairesFilter = new FormControl();
  objet = new FormControl();
  dateDebut = new FormControl();
  dateFin = new FormControl();
  expiredFilter = new FormControl();
  filteredValues = { dateEnvoi: '', type: '', objet: '', finValidite: '', destinataires: '', expired: '' };

  selectedValue: string;
  types: any;
  loading = false;

  fileInfos: any;
  transfers: Array<any> = [];
  validUntilDate;
  maxDate = new Date();
  public selectedDate: Date = new Date();
  destDownload: number;
  roots: string[] = [];
  plisPaginated;
  totalItems;
  pageSize = 5;
  pageIndex = 0;
  urlExport;
  donwloadKey: null;
  exportEnCours;
  exportTermine;
  exportEnError;
  interval;

  constructor(
    private _adminService: AdminService,
    private loginService: LoginService,
    private _router: Router,
    private responsiveService: ResponsiveService,
    private _translate: TranslateService, private loaderService: LoaderService,
    private translateService:TranslateService
  ) {
    this.getListTypes();
    this.translateService.onLangChange.subscribe((event: LangChangeEvent) => {
      this.updateTranslations();
    });
  }

  ngOnInit(): void {
    this.updateTranslations();
    this.subscriptions.push(this.responsiveService.getMobileStatus().subscribe(isMobile => {
      this.isMobile = isMobile;
      this.screenWidth = this.responsiveService.screenWidth;
    }));

    if (!this.loginService.isLoggedIn()) {
      this.navigateToConnect();
    } else {
      //---------------get infos--------------
      this.getPlisInfo(this.pageIndex,this.pageSize);

    }
    this.hideSubscription = this.loaderService.hideSubject.subscribe(hide => {
      if (hide) {
        clearInterval(this.interval);
      }
    });

    this.dataSource.filterPredicate = this.customFilterPredicate();
  }

  private updateTranslations() {
    this.translateService.get('ExportEnCours').subscribe((translation: string) => {
      this.exportEnCours = translation;
    });
    this.translateService.get('ExportTerminé').subscribe((translation: string) => {
      this.exportTermine = translation;
    });
    this.translateService.get('ExportEnError').subscribe((translation: string) => {
      this.exportEnError = translation;
    });
  }

  PdfGenerated(enclosureId: string): any {
    this._adminService
    .getFileInfosConnect({
      senderMail: this.loginService.tokenInfo.getValue().senderMail,
      senderToken: this.loginService.tokenInfo.getValue().senderToken,
    }, enclosureId)
    .pipe(take(1))
    .subscribe(fileInfos => {
      this.fileInfos = fileInfos;
      this.fileInfos.rootFiles.map(file => {
        this.transfers.push({ ...file, folder: false } as FTTransferModel<Transfer>);
      });
      this.fileInfos.rootDirs.map(file => {
        this.transfers.push({ ...file, size: file.totalSize, folder: true } as FTTransferModel<Transfer>);
      });
      this.validUntilDate = new FormControl(new Date(this.fileInfos.validUntilDate));
      this.PDFinfo()
    });


  }

  navigateToConnect() {
    this.loginService.logout();
  }

  navigateToLogin() {
    this._router.navigate(['/connect']);
  }

  reinitialiserFiltres(){
    this.destinatairesFilter.setValue(null);
    this.dateDebut.setValue(null);
    this.dateFin.setValue(null);
    this.objet.setValue(null);
    this.expiredFilter.setValue(null);
    this.pageIndex = 0;
  }

  getPlisInfo(page:number, pageSize: number){
    this.loading = true;
    this.dataSource.data = [];
    this.empList = [];
    this._adminService.getPlisSent(
      {
        senderMail: this.loginService.tokenInfo.getValue().senderMail,
        senderToken: this.loginService.tokenInfo.getValue().senderToken
      }, page, pageSize,this.destinatairesFilter.value,moment(this.dateDebut.value).utc(true).toISOString(), moment(this.dateFin.value).utc(true).toISOString(), this.objet.value, this.expiredFilter.value
    ).pipe(take(1)).subscribe(
      {
        next: (response) => {
          {
            const fileInfos = response.plis;
            this.plisPaginated = response;
            this.pageSize = pageSize;
            this.totalItems = this.plisPaginated.totalItems;
            fileInfos?.forEach(t => {
              this.destDownload = 0;
              this.roots = [];
              //-----------condition on type-----------
              let type = "";
              if (t.publicLink) {
                type = 'Lien';
              }
              else {
                type = 'Courriel';
              }
              //-----------condition on expired-----------
              let expired = "";
              let matTooltip = "";
              if (t.expired) {
                expired = 'remove_red_eye';
                this._translate.stream('Details_Oeil').pipe(take(1)).subscribe(v => {
                  matTooltip = v;
                })
              }
              else {
                expired = 'edit';
                this._translate.stream('Details_Edit').pipe(take(1)).subscribe(v => {
                  matTooltip = v;
                })
              }

              const destinataires = t.recipientsMails.map(n => n.recipientMail).join(", ");

              const taillePli = t.totalSize.split(" ");

              t.recipientsMails.forEach((element) => {
                element.numberOfDownloadPerRecipient>0 ? this.destDownload++: "";
              });

              t.rootFiles.forEach((element) => {
                this.roots.push(element.name);
              });

              t.rootDirs.forEach((element) => {
                this.roots.push(element.name);
              });

              //---------add to mat-table-------------
              this.empList.push({
                dateEnvoi: t.timestamp, type: type, objet: t.subject,
                taille: taillePli[0], typeSize: taillePli[1], finValidite: t.validUntilDate, destinataires: destinataires,
                enclosureId: t.enclosureId, expired: expired, matTooltip: matTooltip, nombreDest: t.recipientsMails.length,
                destDownload: this.destDownload, roots: this.roots,
              });
              this.empList.sort((a,b) => Date.parse(b.dateEnvoi) - Date.parse(a.dateEnvoi))
              this.dataSource.data = this.empList;

            });

          }
          this.loading = false;
        },
        error: (err) => {
          console.error(err);
          this.navigateToConnect();
        }
      }
    );
  }

  getListTypes() {
    this._translate.stream("typePli").subscribe(v => {
      this.types = v
    });
  }


  customFilterPredicate() {
    const myFilterPredicate = function (data: PliModel, filter: string): boolean {
      let searchString = JSON.parse(filter);
      let nameFound = data.destinataires.toString().trim().toLowerCase().indexOf(searchString.destinataires.toLowerCase()) !== -1
        || data.type.toString().trim().toLowerCase().indexOf(searchString.type.toLowerCase()) !== -1
        || data.objet.toString().trim().toLowerCase().indexOf(searchString.objet.toLowerCase()) !== -1;
      let positionFound = data.expired.toString().trim().toLowerCase().indexOf(searchString.expired.toLowerCase()) !== -1
      if (searchString.topFilter) {
        return nameFound || positionFound
      } else {
        return nameFound && positionFound
      }
    }
    return myFilterPredicate;
  }

  isLoggedIn() {
    return this.loginService.isLoggedIn();
  }



  //-------------navigate token------------

  navigateTo(enclosureId: String) {

    this._router.navigate(['/admin'], {
      queryParams: {
        enclosure: enclosureId
      },
      queryParamsHandling: 'merge',
    });

  }

  backToHome() {
    this._router.navigate(['/upload']);
  }

  ngAfterViewInit() {
    //this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  filterType(filterValue: string) {
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }

  applyFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }


  get translate(): TranslateService {
    return this._translate;
  }


  exportCSV() {
    this.loaderService.show(this.exportEnCours, undefined,true);
    this.urlExport = null;
    this.donwloadKey = null;
    this._adminService.exportPlis({
        senderMail: this.loginService.tokenInfo.getValue().senderMail,
        senderToken: this.loginService.tokenInfo.getValue().senderToken
      },
      this.destinatairesFilter.value,
      moment(this.dateDebut.value).utc(true).toISOString(),
      moment(this.dateFin.value).utc(true).toISOString(),
      this.objet.value,
      this.expiredFilter.value,
      true
    ).pipe(take(1)).subscribe(
      response => {
        this.donwloadKey = response;
      },
      error => {
        this.loaderService.show(this.exportEnError, undefined, false);
        setTimeout(() => this.loaderService.hide(), 3000);
      }
    );


    this.interval = setInterval(() => {
      if(this.donwloadKey != null && this.donwloadKey != undefined){
        this._adminService.getUrlExport({
            senderMail: this.loginService.tokenInfo.getValue().senderMail,
            senderToken: this.loginService.tokenInfo.getValue().senderToken
          }, this.donwloadKey
        ).pipe(take(1)).subscribe(
          urlResult => {
            if (urlResult != null && urlResult != "") {
              this.urlExport = urlResult;
              this.loaderService.show(this.exportTermine, this.donwloadKey, false);
              clearInterval(this.interval); // Arrête l'intervalle une fois que l'URL est obtenue
            }
          },
          error => {
            this.loaderService.show(this.exportEnError, undefined, false);
            setTimeout(() => this.loaderService.hide(), 5000);
          }
        );
      }
    }, 30000);
  }


  ngOnDestroy() {
    this.subscriptions.forEach(x => {
      x.unsubscribe();
    });
    if (this.interval) {
      clearInterval(this.interval);
    }
    if (this.hideSubscription) {
      this.hideSubscription.unsubscribe();
    }
  }

  PDFinfo(){
    this._adminService.generatePDF(this.fileInfos, "namePDF-sent")
  }

  onPageChange(event: PageEvent, isEvent) {
    if(isEvent){
      this.pageSize = event.pageSize;
      this.pageIndex = event.pageIndex;
    }
    this.getPlisInfo(this.pageIndex,this.pageSize);
  }

  filtrer(){
    this.pageIndex = 0;
    this.paginator.pageIndex = this.pageIndex;
    this.onPageChange(null, false);
  }
}


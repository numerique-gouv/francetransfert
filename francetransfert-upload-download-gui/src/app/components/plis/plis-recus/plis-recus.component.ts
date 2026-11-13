/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */


import { Component, HostListener, OnDestroy, effect, inject, viewChild, signal } from '@angular/core';
import { MatPaginator, MatPaginatorIntl } from '@angular/material/paginator';
import { MatTableDataSource, MatTable, MatColumnDef, MatHeaderCellDef, MatHeaderCell, MatCellDef, MatCell, MatHeaderRowDef, MatHeaderRow, MatRowDef, MatRow } from '@angular/material/table';
import { LangChangeEvent, TranslateService, TranslatePipe } from '@ngx-translate/core';
import { AdminService, ResponsiveService } from 'src/app/services';
import { LoginService } from 'src/app/services/login/login.service';
import { Subscription, take } from 'rxjs';
import { Router } from '@angular/router';
import { MatSort } from '@angular/material/sort';
import { PliRecuModel } from 'src/app/models/pli-recu.model';
import { FormBuilder, FormControl, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Transfer } from '@flowjs/ngx-flow';
import { FTTransferModel } from 'src/app/models';
import { PageEvent } from "@angular/material/paginator";
import { CustomPaginatorService } from "../../../shared/custom-paginator/custom-paginator.service";
import { LoaderService } from "../../../services/loader/loader.service";
import { dateValidator, passwordValidator } from "../../../shared/validators/forms-validator";
import moment from 'moment';
import { DateAdapter } from "@angular/material/core";
import { MyDateAdapter, MyDefaultDateAdapter } from "../../upload/envelope/envelope-parameters-form/my-date-adapter";
import { environment } from '../../../../environments/environment';
import { ConfigService } from '../../../services/config/config.service';
import { MatFormField, MatLabel, MatSuffix, MatSelect } from '@angular/material/select';
import { MatInput } from '@angular/material/input';
import { MatDatepickerInput, MatDatepicker } from '@angular/material/datepicker';
import { MatIconButton, MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatOption } from '@angular/material/autocomplete';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatTooltip } from '@angular/material/tooltip';
import { MatCardActions } from '@angular/material/card';
import { DatePipe } from '@angular/common';

@Component({
    selector: 'ft-plis-recus',
    templateUrl: './plis-recus.component.html',
    styleUrls: ['./plis-recus.component.scss'],
    providers: [{ provide: MatPaginatorIntl, useClass: CustomPaginatorService }, { provide: DateAdapter, useClass: MyDefaultDateAdapter }],
    imports: [MatFormField, MatLabel, MatInput, ReactiveFormsModule, MatDatepickerInput, MatDatepicker, MatIconButton, MatSuffix, MatIcon, MatSelect, MatOption, MatButton, MatProgressSpinner, MatTable, MatSort, MatColumnDef, MatHeaderCellDef, MatHeaderCell, MatCellDef, MatCell, MatTooltip, MatHeaderRowDef, MatHeaderRow, MatRowDef, MatRow, MatPaginator, MatCardActions, DatePipe, TranslatePipe]
})
export class PlisRecusComponent {
  private _adminService = inject(AdminService);
  private loginService = inject(LoginService);
  private _router = inject(Router);
  private responsiveService = inject(ResponsiveService);
  private _translate = inject(TranslateService);
  private loaderService = inject(LoaderService);
  private configService = inject(ConfigService);


  readonly sort = viewChild(MatSort);
  readonly paginator = viewChild(MatPaginator);
  empList: PliRecuModel[] = [];
  displayedColumns: string[] = ['dateReception', 'expediteur', 'objet', 'taille', 'finValidite', 'expired'];
  dataSource = new MatTableDataSource<PliRecuModel>(this.empList);
  lengthScreen: any;
  link: string;
  destinatairesFilter = new FormControl();
  expiredFilter = new FormControl();
  objet = new FormControl();
  dateDebut = new FormControl();
  dateFin = new FormControl();
  filteredValues = { dateReception: '', expediteur: '', objet: '', taille: '', finValidite: '', expired: '' };
  readonly types = signal<any>(undefined);
  subscriptions: Subscription[] = [];

  readonly loading = signal(false);
  fileInfos: any;
  transfers: Array<any> = [];
  validUntilDate;
  public selectedDate: Date = new Date();
  maxDate = new Date();
  numberOfDownload: any;
  numberOfDownloadList: any;
  roots: string[] = [];
  downloadDates: any;
  plisPaginated;
  readonly totalItems = signal<number>(undefined);
  pageSize = 5;
  pageIndex = 0;
  urlExport;
  checkingUrlSubscription: Subscription;
  donwloadKey;
  exportEnCours;
  exportTermine;
  exportEnError;
  interval;



  constructor() {
    this.getListTypes();
    this._translate.onLangChange.subscribe((event: LangChangeEvent) => {
      this.updateTranslations();
    });
    effect(() => {
      if (!this.loaderService.isLoading()) {
        clearInterval(this.interval);
      }
    });
  }

  ngOnInit(): void {
    this.updateTranslations();
    this.empList = [];
    this.onResize();

    //---------------get infos--------------
    if (!this.loginService.isLoggedIn()) {
      this.navigateToConnect();
    } else {
      this.getPlisInfo(this.pageIndex, this.pageSize);
    }

  }

  private updateTranslations() {
    this._translate.get('ExportEnCours').subscribe((translation: string) => {
      this.exportEnCours = translation;
    });
    this._translate.get('ExportTerminé').subscribe((translation: string) => {
      this.exportTermine = translation;
    });
    this._translate.get('ExportEnError').subscribe((translation: string) => {
      this.exportEnError = translation;
    });
  }



  PdfGenerated(enclosureId: string): any {
    this._adminService
      .getFileInfosReciever({
        enclosure: enclosureId,
        senderMail: this.loginService.tokenInfo().senderMail,
        senderToken: this.loginService.tokenInfo().senderToken,
      })
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
        let temp = new Date(this.fileInfos.timestamp);
        this.selectedDate = temp;
        this.maxDate.setDate(temp.getDate() + this.configService.configInfo().uploadExpiredLimit);
        this.fileInfos.recipientsMails.forEach((element) => {
          this.numberOfDownload = element.numberOfDownloadPerRecipient;
          this.downloadDates = element.downloadDates;
        });
        this.PDFinfo();
      });

  }

  PDFinfo() {
    this._adminService.generatePDF(this.fileInfos, "namePDF-recieved")
  }

  //-------------navigate token------------
  navigateTo(enclosureId: String, link: String) {
    this.link = "/" + link;

    this._router.navigate([this.link], {
      queryParams: {
        enclosure: enclosureId,
        recieved: false,
      },
      queryParamsHandling: 'merge',
    });
  }

  navigateToDetails(enclosureId: String) {
    this._router.navigate(['/détail-pli'], {
      queryParams: {
        enclosure: enclosureId
      },
      queryParamsHandling: 'merge',
    });
  }


  backToHome() {
    this._router.navigate(['/upload']);
  }

  filtrer() {
    this.pageIndex = 0;
    //this.pageSize = 5;
    //this.paginator.pageSize = this.pageSize;
    this.paginator().pageIndex = this.pageIndex;
    //this.paginator._changePageSize(this.pageSize);
    this.onPageChange(null, false);
  }

  getPlisInfo(page: number, size: number) {
    this.loading.set(true);
    this.dataSource.data = [];
    this.empList = [];
    this._adminService.getPlisReceived(
      {
        receiverMail: this.loginService.tokenInfo().senderMail,
        senderToken: this.loginService.tokenInfo().senderToken
      }, page, size, this.destinatairesFilter.value, moment(this.dateDebut.value).utc(true).toISOString(), moment(this.dateFin.value).utc(true).toISOString(), this.objet.value, this.expiredFilter.value

    ).pipe(take(1)).subscribe(
      {
        next: response => {
          const fileInfos = response.plis;
          this.plisPaginated = response;
          this.pageSize = size;
          this.totalItems.set(this.plisPaginated.totalItems);
          fileInfos?.forEach(t => {
            const taillePli = t.totalSize.split(" ");
            this.roots = [];
            this.numberOfDownloadList = 0;
            //-----------condition on expired-----------
            let expired = "";
            let matTooltip = "";
            if (t.expired) {
              expired = 'remove_red_eye';
            }
            else {
              expired = 'cloud_download';
            }


            t.rootFiles.forEach((element) => {
              this.roots.push(element.name);
            });

            t.rootDirs.forEach((element) => {
              this.roots.push(element.name);
            });

            t.recipientsMails.forEach((element) => {
              this.downloadDates = element.downloadDates;
              this.numberOfDownloadList = this.numberOfDownloadList + element.numberOfDownloadPerRecipient;
            });

            //var mostRecentDate = undefined;
            //var mostOldDate = undefined;

            var mostRecentDate = this.downloadDates.length > 0 ? this.downloadDates.reduce(function (a, b) { return a > b ? a : b; }) : "";
            var mostOldDate = this.downloadDates.length > 0 ? this.downloadDates.reduce(function (a, b) { return a < b ? a : b; }) : "";
            //---------add to mat-table-------------
            this.empList.push({
              dateReception: t.timestamp,
              expediteur: t.senderEmail, objet: t.subject,
              taille: taillePli[0], typeSize: taillePli[1], finValidite: t.validUntilDate,
              enclosureId: t.enclosureId, expired: expired, matTooltip: matTooltip, downloadCount: this.numberOfDownloadList,
              firstDate: mostOldDate, lastDate: mostRecentDate, roots: this.roots,
            });
            this.empList.sort((a, b) => Date.parse(b.dateReception) - Date.parse(a.dateReception))
            this.dataSource.data = this.empList;
          });
          this.loading.set(false);
        },
        error: (err) => {
          console.error(err);
          this.navigateToConnect();
        }
      });
  }

  getListTypes() {
    this._translate.stream("typePliReçus").subscribe(v => {
      this.types.set(v)
    });
  }

  customFilterPredicate() {
    const myFilterPredicate = function (data: PliRecuModel, filter: string): boolean {
      let searchString = JSON.parse(filter);
      let nameFound = data.expediteur.toString().trim().toLowerCase().indexOf(searchString.expediteur.toLowerCase()) !== -1
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

  onResize() {
    this.responsiveService.checkWidth();
    if (this.responsiveService.screenWidth() === 'lg') {
      this.lengthScreen = 150
    }
    else {
      this.lengthScreen = 50

    }
  }

  exportCSV() {
    this.loaderService.show(this.exportEnCours, undefined, true);
    this.urlExport = null;
    this.donwloadKey = null;
    this._adminService.exportPlis({
      senderMail: this.loginService.tokenInfo().senderMail,
      senderToken: this.loginService.tokenInfo().senderToken
    },
      this.destinatairesFilter.value,
      moment(this.dateDebut.value).utc(true).toISOString(),
      moment(this.dateFin.value).utc(true).toISOString(),
      this.objet.value,
      this.expiredFilter.value,
      false
    ).pipe(take(1)).subscribe(
      response => {
        this.donwloadKey = response;
      },
      error => {
        this.loaderService.show(this.exportEnError, undefined, false);
        setTimeout(() => this.loaderService.hide(), 5000);
      }
    );


    this.interval = setInterval(() => {
      if (this.donwloadKey != null && this.donwloadKey != undefined) {
        this._adminService.getUrlExport({
          senderMail: this.loginService.tokenInfo().senderMail,
          senderToken: this.loginService.tokenInfo().senderToken
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


  reinitialiserFiltres() {
    this.destinatairesFilter.setValue(null);
    this.dateDebut.setValue(null);
    this.dateFin.setValue(null);
    this.objet.setValue(null);
    this.expiredFilter.setValue(null);
  }
  ngAfterViewInit() {
    this.dataSource.sort = this.sort();
  }

  applyFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }

  navigateToConnect() {
    this.loginService.logout();
  }

  navigateToLogin() {
    this._router.navigate(['/connect']);
  }

  isLoggedIn() {
    return this.loginService.isLoggedIn();
  }

  get translate(): TranslateService {
    return this._translate;
  }

  onPageChange(event: PageEvent, isEvent) {
    if (isEvent) {
      this.pageSize = event.pageSize;
      this.pageIndex = event.pageIndex;
    }
    this.getPlisInfo(this.pageIndex, this.pageSize)
  }

  ngOnDestroy() {
    this.subscriptions.forEach(x => {
      x.unsubscribe();
    });
    if (this.interval) {
      clearInterval(this.interval);
    }
  }

  getDisplayUrl(url: string | null): string {
    if (!url) return '';

    const maxLength = 50;
    return url.length > maxLength ? url.substring(0, maxLength) + '...' : url;
  }

  handleUrlClick(url: string | null): void {
    if (url) {
      window.open(url, '_blank');
    }
  }

}





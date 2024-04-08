/*
  * Copyright (c) Ministère de la Culture (2022)
  *
  * SPDX-License-Identifier: MIT
  * License-Filename: LICENSE.txt
  */


import { ChangeDetectorRef, Component, HostListener, OnDestroy, ViewChild } from '@angular/core';
import { MatLegacyPaginator as MatPaginator, MatLegacyPaginatorIntl as MatPaginatorIntl } from '@angular/material/legacy-paginator';
import { MatLegacyTableDataSource as MatTableDataSource } from '@angular/material/legacy-table';
import { TranslateService } from '@ngx-translate/core';
import { AdminService, ResponsiveService } from 'src/app/services';
import { LoginService } from 'src/app/services/login/login.service';
import { Subscription, take } from 'rxjs';
import { Router } from '@angular/router';
import { MatSort } from '@angular/material/sort';
import { PliRecuModel } from 'src/app/models/pli-recu.model';
import { FormControl } from '@angular/forms';
import { Transfer } from '@flowjs/ngx-flow';
import { FTTransferModel } from 'src/app/models';

@Component({
  selector: 'ft-plis-recus',
  templateUrl: './plis-recus.component.html',
  styleUrls: ['./plis-recus.component.scss']
})
export class PlisRecusComponent {



  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  responsiveSubscription: Subscription = new Subscription;
  empList: PliRecuModel[] = [];
  displayedColumns: string[] = ['dateReception', 'expediteur', 'objet', 'taille', 'finValidite', 'expired'];
  dataSource = new MatTableDataSource<PliRecuModel>(this.empList);
  isMobile: boolean = false;
  lengthScreen: any;
  screenWidth: string;
  link: string;

  destinatairesFilter = new FormControl();
  expiredFilter = new FormControl();
  filteredValues = { dateReception: '', expediteur: '', objet: '', taille: '', finValidite: '', expired: '' };
  types: any;
  subscriptions: Subscription[] = [];

  loading = false;
  fileInfos: any;
  transfers: Array<any> = [];
  validUntilDate;
  public selectedDate: Date = new Date();
  maxDate = new Date();
  numberOfDownload: any;
  numberOfDownloadList: any;
  roots: string[] = [];
  downloadDates: any;

  constructor(
    private _adminService: AdminService,
    private loginService: LoginService,
    private _router: Router,
    private responsiveService: ResponsiveService,
    private _translate: TranslateService,
  ) {
    this.getListTypes();
  }


  PdfGenerated(enclosureId: string): any {
    this._adminService
      .getFileInfosReciever({
        enclosure: enclosureId,
        senderMail: this.loginService.tokenInfo.getValue().senderMail,
        senderToken: this.loginService.tokenInfo.getValue().senderToken,
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
        this.maxDate.setDate(temp.getDate() + 90);
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



  ngOnInit(): void {


    this.empList = [];
    this.responsiveSubscription = this.responsiveService.getMobileStatus().subscribe(isMobile => {
      this.isMobile = isMobile;
      this.screenWidth = this.responsiveService.screenWidth;

    });
    this.onResize();

    //---------------get infos--------------
    if (!this.loginService.isLoggedIn()) {
      this.navigateToConnect();
    } else {
      this.loading = true;
      this._adminService.getPlisReceived(
        {
          receiverMail: this.loginService.tokenInfo.getValue().senderMail,
          senderToken: this.loginService.tokenInfo.getValue().senderToken
        }

      ).pipe(take(1)).subscribe(
        {
          next:
            fileInfos => {
              fileInfos.forEach(t => {

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
              this.loading = false;
            },
          error: (err) => {
            console.error(err);
            this.navigateToConnect();
          }
        });
    }
    this.subscriptions.push(this.destinatairesFilter.valueChanges.subscribe((nameFilterValue) => {
      this.filteredValues['expediteur'] = nameFilterValue;
      this.filteredValues['objet'] = nameFilterValue;
      this.dataSource.filter = JSON.stringify(this.filteredValues);
    }));

    this.subscriptions.push(this.expiredFilter.valueChanges.subscribe((expiredFilterValue) => {
      this.filteredValues['expired'] = expiredFilterValue;
      this.dataSource.filter = JSON.stringify(this.filteredValues);
    }));

    this.dataSource.filterPredicate = this.customFilterPredicate();

  }

  getListTypes() {
    this._translate.stream("typePliReçus").subscribe(v => {
      this.types = v
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
    if (this.screenWidth === 'lg') {
      this.lengthScreen = 150
    }
    else {
      this.lengthScreen = 50

    }
  }

  exportCSV() {
    this._adminService.exportCSV(this.empList, "recieved")
  }

  ngAfterViewInit() {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
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

  ngOnDestroy() {
    this.responsiveSubscription.unsubscribe();
    this.subscriptions.forEach(x => {
      x.unsubscribe();
    });
  }

}





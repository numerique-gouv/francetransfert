/*
  * Copyright (c) Ministère de la Culture (2022)
  *
  * SPDX-License-Identifier: MIT
  * License-Filename: LICENSE.txt
  */

import { Component, ViewChild } from '@angular/core';
import { MatLegacyPaginator as MatPaginator, MatLegacyPaginatorIntl as MatPaginatorIntl } from '@angular/material/legacy-paginator';
import { MatLegacyTableDataSource as MatTableDataSource } from '@angular/material/legacy-table';
import { TranslateService } from '@ngx-translate/core';
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


interface Type {
  value: string;
  viewValue: string;
}


@Component({
  selector: 'ft-plis-envoyes',
  templateUrl: './plis-envoyes.component.html',
  styleUrls: ['./plis-envoyes.component.scss']
})
export class PlisEnvoyesComponent {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  empList: PliModel[] = [];
  displayedColumns: string[] = ['dateEnvoi', 'type', 'objet', 'taille', 'finValidite', 'destinataires', 'expired'];
  dataSource = new MatTableDataSource<PliModel>(this.empList);
  subscriptions: Subscription[] = [];
  isMobile;
  screenWidth;

  destinatairesFilter = new FormControl();
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

  constructor(
    private _adminService: AdminService,
    private loginService: LoginService,
    private _router: Router,
    private responsiveService: ResponsiveService,
    private _translate: TranslateService
  ) {
    this.getListTypes();

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

  ngOnInit(): void {

    this.subscriptions.push(this.responsiveService.getMobileStatus().subscribe(isMobile => {
      this.isMobile = isMobile;
      this.screenWidth = this.responsiveService.screenWidth;
    }));

    if (!this.loginService.isLoggedIn()) {
      this.navigateToConnect();
    } else {
      this.loading = true;
      //---------------get infos--------------
      this._adminService.getPlisSent(
        {
          senderMail: this.loginService.tokenInfo.getValue().senderMail,
          senderToken: this.loginService.tokenInfo.getValue().senderToken
        }

      ).pipe(take(1)).subscribe(
        {
          next: (fileInfos) => {
            {
              fileInfos.forEach(t => {
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

    this.subscriptions.push(this.destinatairesFilter.valueChanges.subscribe((nameFilterValue) => {
      this.filteredValues['type'] = nameFilterValue;
      this.filteredValues['objet'] = nameFilterValue;
      this.filteredValues['destinataires'] = nameFilterValue;
      this.dataSource.filter = JSON.stringify(this.filteredValues);
    }));

    this.subscriptions.push(this.expiredFilter.valueChanges.subscribe((expiredFilterValue) => {
      this.filteredValues['expired'] = expiredFilterValue;
      this.dataSource.filter = JSON.stringify(this.filteredValues);
    }));

    this.dataSource.filterPredicate = this.customFilterPredicate();
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
    this.dataSource.paginator = this.paginator;
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
    this._adminService.exportCSV(this.empList, "sent")
  }

  ngOnDestroy() {
    this.subscriptions.forEach(x => {
      x.unsubscribe();
    });
  }

  PDFinfo(){
    this._adminService.generatePDF(this.fileInfos, "namePDF-sent")
  }

}


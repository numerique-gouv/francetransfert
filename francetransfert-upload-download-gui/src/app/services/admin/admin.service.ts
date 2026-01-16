/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Transfer } from '@flowjs/ngx-flow';
import { TranslateService } from '@ngx-translate/core';
import jsPDF from 'jspdf';
import { BehaviorSubject, Observable, of, Subject, take, throwError, timer } from 'rxjs';
import { catchError } from 'rxjs/internal/operators/catchError';
import { map } from 'rxjs/internal/operators/map';
import { FTTransferModel } from 'src/app/models';
import { PliDestinataires } from 'src/app/models/pli-destinataires.model';
import { TokenModel } from 'src/app/models/token.model';
import { FileSizePipe, FileTypePipe, FileUnitPipe } from 'src/app/shared/pipes';
import { environment } from 'src/environments/environment';
import { LoginService } from '../login/login.service';
import * as pdfMake from "pdfmake/build/pdfmake";
import * as pdfFonts from "pdfmake/build/vfs_fonts";
import { switchMap, takeWhile } from 'rxjs/operators';
//import autoTable from 'jspdf-autotable';
(pdfMake as any).vfs = pdfFonts.pdfMake.vfs;

@Injectable({
  providedIn: 'root'
})
export class AdminService {

  fileInfos: any;
  transfers: Array<any> = [];
  validUntilDate;
  maxDate = new Date();
  public selectedDate: Date = new Date();
  event: any;
  downloadDate: any;
  newUnit: any;
  unitSize: string;
  transfer: Array<any> = [];
  transferFolder: Array<any> = [];
  now = new Date();


  adminError$: BehaviorSubject<number> = new BehaviorSubject<number>(null);
  currentDestinatairesInfo: BehaviorSubject<PliDestinataires> = new BehaviorSubject<PliDestinataires>(null);
  mockCsvData: string;
  header: any;
  fileTitle: string;

  dataSubject: Subject<any> = new Subject<any>();

  constructor(private _httpClient: HttpClient,
    private loginService: LoginService,
    private _translate: TranslateService,
    private pipe: FileUnitPipe,
    private filePipe: FileTypePipe,
    private sizePipe: FileSizePipe,
  ) { }


  getFileInfosConnect(body: any, enclosureId: string): Observable<any> {
    const treeBody = {
      senderMail: body.senderMail,
      senderToken: body.senderToken,
    };
    return this._httpClient.post(
      `${environment.host}${environment.apis.admin.fileInfosConnect}?enclosure=${enclosureId}`,
      treeBody
    ).pipe(map(response => {
      this.adminError$.next(null);
      return response;
    }),
      catchError(this.handleError('file-info-connect'))
    );
  }


  getFileInfosReciever(params: any) {

    const body = {
      enclosure: params['enclosure'],
      token: params['senderToken'],
      senderMail: params['senderMail'],
    };

    return this._httpClient.post(
      `${environment.host}${environment.apis.admin.fileInfosReciever}`,
      body
    ).pipe(map(response => {
      this.adminError$.next(null);
      return response;
    }),
      catchError(this.handleError('file-info-reciever'))
    );
  }
  getPlisSent(body: any, page: number, size: number, searshedMail, dateDebut, dateFin, objet, statut): Observable<any> {
    const treeBody = {
      senderMail: body.senderMail,
      senderToken: body.senderToken,
    };

    let params = new HttpParams()
      .set('page', page?.toString())
      .set('size', size?.toString());

    if (searshedMail) {
      params = params.set('searchedMail', searshedMail.toString());
    }

    if (dateDebut) {
      params = params.set('dateDebut', dateDebut);
    }

    if (dateFin) {
      params = params.set('dateFin', dateFin);
    }

    if (objet) {
      params = params.set('objet', objet);
    }

    if (statut) {
      params = params.set('statut', statut);
    }
    return this._httpClient.post(
      `${environment.host}${environment.apis.admin.getPlisSent}`,
      treeBody, { params: params }
    ).pipe(map((response) => {
      return response;
    }));
  }

  exportPlis(body: any, searshedMail, dateDebut, dateFin, objet, statut, isPliSent): Observable<any> {
    const treeBody = {
      senderMail: body.senderMail,
      senderToken: body.senderToken,
    };

    let params = new HttpParams();

    if (searshedMail) {
      params = params.set('searchedMail', searshedMail.toString());
    }

    if (dateDebut) {
      params = params.set('dateDebut', dateDebut);
    }

    if (dateFin) {
      params = params.set('dateFin', dateFin);
    }


    if (objet) {
      params = params.set('objet', objet);
    }

    if (statut) {
      params = params.set('statut', statut);
    }

    params = params.set('isPliSent', isPliSent);

    return this._httpClient.post(`${environment.host}${environment.apis.admin.export}`, treeBody, { params: params, responseType: 'text' }).pipe(
      map((response: string) => {
        return response;
      }),
      catchError(error => {
        console.error('Error in getUrlExport:', error);
        return of('');
      })
    );

  }


  getFileInfos(params: Array<{ string: string }>) {

    const body = {
      enclosure: params['enclosure'],
      token: params['token'],
    }

    return this._httpClient.post(
      `${environment.host}${environment.apis.admin.fileInfos}`,
      body
    ).pipe(map(response => {
      this.adminError$.next(null);
      return response;
    }),
      catchError(this.handleError('file-info'))
    );
  }


  deleteFile(body) {

    return this._httpClient.post(
      `${environment.host}${environment.apis.admin.deleteFile}`,
      body
    ).pipe(map(response => {
      this.adminError$.next(null);
      return response;
    }),
      catchError(this.handleError('delete-file'))
    );
  }

  updateExpiredDate(body: any): any {
    return this._httpClient.post(`${environment.host}${environment.apis.admin.updateExpiredDate}`, {
      enclosureId: body.enclosureId,
      newDate: body.newDate,
      token: body.token,
      senderMail: body.senderMail,
    }).pipe(map(response => {
      this.adminError$.next(null);
      return response;
    }),
      catchError(this.handleError('update-expired-date'))
    );
  }

  resendLink(body) {
    return this._httpClient.post(`${environment.host}${environment.apis.admin.resendLink}`, {
      enclosureId: body.enclosureId,
      token: body.token,
      newRecipient: body.recipient,
      senderMail: body.senderMail,
    }).pipe(map(response => {
      this.adminError$.next(null);
      return response;
    }),
      catchError(this.handleError('resend-Link'))
    );

  }


  addNewRecipient(body) {
    return this._httpClient.post(`${environment.host}${environment.apis.admin.addNewRecipient}`, {
      enclosureId: body.enclosureId,
      token: body.token,
      newRecipient: body.newRecipient,
      senderMail: body.senderMail,
    }).pipe(map(response => {
      this.adminError$.next(null);
      return response;
    }),
      catchError(this.handleError('add-new-Recipient'))
    );
  }

  deleteRecipient(body) {
    return this._httpClient.post(`${environment.host}${environment.apis.admin.deleteRecipient}`, {
      enclosureId: body.enclosureId,
      token: body.token,
      newRecipient: body.newRecipient,
      senderMail: body.senderMail,
    }).pipe(map(response => {
      this.adminError$.next(null);
      return response;
    }),
      catchError(this.handleError('delete-Recipient'))
    );
  }

  getPlisReceived(body: any, page, size, searshedMail, dateDebut, dateFin, objet, statut): Observable<any> {
    const treeBody = {
      senderMail: body.receiverMail,
      senderToken: body.senderToken,
    };
    debugger
    let params = new HttpParams()
      .set('page', page?.toString())
      .set('size', size?.toString());

    if (searshedMail) {
      params = params.set('searchedMail', searshedMail.toString());
    }

    if (dateDebut) {
      params = params.set('dateDebut', dateDebut);
    }

    if (dateFin) {
      params = params.set('dateFin', dateFin);
    }



    if (objet) {
      params = params.set('objet', objet);
    }

    if (statut) {
      params = params.set('statut', statut);
    }

    return this._httpClient.post(
      `${environment.host}${environment.apis.admin.getPlisReceived}`,
      treeBody, { params: params }
    ).pipe(map((response) => {
      return response;
    }));
  }

  getUrlExport(body: any, downloadKey): Observable<any> {
    const treeBody = {
      senderMail: body.senderMail,
      senderToken: body.senderToken,
    };
    let params = new HttpParams()
      .set('objectKey', downloadKey);

    return this._httpClient.post(`${environment.host}${environment.apis.admin.urlExport}?objectKey=${downloadKey}`, treeBody, { responseType: 'text' }).pipe(
      map((response: string) => {
        return response;
      }),
      catchError(error => {
        console.error('Error in getUrlExport:', error);
        return of('');
      })
    );
  }


  private isValidUrl(url: string): boolean {
    try {
      new URL(url); // Vérifie si la chaîne est une URL valide
      return true;
    } catch (_) {
      return false;
    }
  }

  setDestinatairesList(destinatairesData) {
    this.currentDestinatairesInfo.next({
      destinataires: destinatairesData.destinataires,
    });
  }

  cleanDestinatairesList() {
    this.currentDestinatairesInfo.next(null);
  }




  convertToCSV(objArray) {
    var array = typeof objArray != 'object' ? JSON.parse(objArray) : objArray;
    var str = '';

    for (var i = 0; i < array.length; i++) {
      var line = '';
      for (var index in array[i]) {
        if (line != '') line += '; '
        line += array[i][index];
      }
      str += line + '\r\n';
    }
    return str;
  }

  formatToCsvData(empList: any, pli: any) {

    pli == "sent" ? this.header = [
      {
        Date_Envoi: this._translate.instant('Date_Envoi'),
        Type: this._translate.instant('Type'),
        Objet: this._translate.instant('Objet'),
        Taille: this._translate.instant('Taille'),
        Fin_Validité: this._translate.instant('Fin_Validité'),
        Liste_Destinataires: this._translate.instant('Liste_Destinataires'),
        NombreDest: this._translate.instant('Nombre_Destinataires'),
        DestDownload: this._translate.instant('Destinataire_Download'),
        Roots: this._translate.instant('Éléments_Pli'),
      },] : this.header = [
        {
          Date_Récéption: this._translate.instant('Date_Réception'),
          Expéditeur: this._translate.instant('Expéditeur'),
          Objet: this._translate.instant('Objet'),
          Taille: this._translate.instant('Taille'),
          Fin_Validité: this._translate.instant('Fin_Validité'),
          Nombre_Téléchargement: this._translate.instant('Nombre_Téléchargement'),
          Date_Premier_Téléchargement: this._translate.instant('Date_Premier_Téléchargement'),
          Date_Dernier_Téléchargement: this._translate.instant('Date_Dernier_Téléchargement'),
          Roots: this._translate.instant('Éléments_Pli'),
        },
      ]
    let itemsFormatted = [];


    empList.forEach((t) => {

      let tempFV = new Date(t.finValidite);
      var selectedDateFV = tempFV;

      if (pli == "sent") {
        this.fileTitle = this._translate.instant('Plis_Envoyés');
        let temp = new Date(t.dateEnvoi);
        var selectedDate = temp;

        itemsFormatted.push({
          dateEnvoi: selectedDate.toLocaleDateString(this._translate.currentLang, {
            hour: "numeric",
            minute: "numeric",
            second: "numeric",
            day: 'numeric',
            month: 'numeric',
            year: 'numeric',
          }),

          type: this._translate.instant(t.type), objet: t.objet ? t.objet : "",
          taille: t.taille + " " + this._translate.instant(t.typeSize),
          finValidite: selectedDateFV.toLocaleDateString(this._translate.currentLang, {
            day: 'numeric',
            month: 'numeric',
            year: 'numeric',
          }),
          downloadCount: t.downloadCount,
          destinataires: t.destinataires.length == 0 ? "" : t.destinataires, nombreDest: t.nombreDest,
          destDownload: t.destDownload, roots: t.roots.length == 0 ? "" : t.roots,

        });
      } else {
        this.fileTitle = this._translate.instant('Plis_Reçus');

        let tempST = new Date(t.dateReception);
        var selectedDateST = tempST;

        let firstDate = new Date(t.firstDate);
        var selectedDateFD = firstDate;

        let lastDate = new Date(t.lastDate);
        var selectedDateLD = lastDate;


        itemsFormatted.push({
          dateReception: selectedDateST.toLocaleDateString(this._translate.currentLang, {
            hour: "numeric",
            minute: "numeric",
            second: "numeric",
            day: 'numeric',
            month: 'numeric',
            year: 'numeric',
          }),

          expediteur: t.expediteur, objet: t.objet ? t.objet : "",
          taille: t.taille + " " + this._translate.instant(t.typeSize),
          finValidite: selectedDateFV.toLocaleDateString(this._translate.currentLang, {
            day: 'numeric',
            month: 'numeric',
            year: 'numeric',
          }),
          downloadCount: t.downloadCount,
          firstDate: t.firstDate ? selectedDateFD.toLocaleDateString(this._translate.currentLang, {
            hour: "numeric",
            minute: "numeric",
            second: "numeric",
            day: 'numeric',
            month: 'numeric',
            year: 'numeric',
          }) : "",
          lastDate: t.lastDate ? selectedDateLD.toLocaleDateString(this._translate.currentLang, {
            hour: "numeric",
            minute: "numeric",
            second: "numeric",
            day: 'numeric',
            month: 'numeric',
            year: 'numeric',
          }) : "",
          roots: t.roots.length == 0 ? "" : t.roots,
        });
      }
    });

    const jsonObject = JSON.stringify(itemsFormatted);
    const csv = this.convertToCSV(jsonObject);

    var array = typeof this.header != 'object' ? JSON.parse(this.header) : this.header;
    var line = '';
    for (var i = 0; i < array.length; i++) {
      for (var index in array[i]) {
        if (line != '') line += '; '
        line += array[i][index];
      }
      line = line + '\r\n';
    }
    var BOM = "\uFEFF";
    this.mockCsvData = BOM + line + csv;

  }


  exportCSV(empList: any, pli: any) {
    this.formatToCsvData(empList, pli)
    const exportedFilenmae = this.fileTitle + '.csv';

    const blob = new Blob([this.mockCsvData], { type: 'text/csv;charset=utf-8;' });

    const link = document.createElement('a');
    if (link.download !== undefined) {
      const url = URL.createObjectURL(blob);
      link.setAttribute('href', url);
      link.setAttribute('download', exportedFilenmae);
      link.style.visibility = 'hidden';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

    }
  }


  private handleError(operation: string) {
    return (err: any) => {
      const errMsg = `error in ${operation}()`;
      if (err instanceof HttpErrorResponse) {
        this.adminError$.next(err.status);
      }
      throw (errMsg);
    };
  }



  async generatePDF(infos: any, typePackage: string, action = "download") {
    this.fileInfos = infos
    this.event = new Date(this.fileInfos.archiveUntilDate)
    var date = new Date(this.fileInfos.validUntilDate);
    date.setHours(0);
    let temp = new Date(this.fileInfos.timestamp);
    this.selectedDate = temp;

    const doc = new jsPDF()
    var img = new Image()
    let docDefinition = {
      content: [

        {
          image: await this.getBase64ImageFromURL(
            "assets/logos/republique_francaise_logo.svg"
          ),
          width: 200,
          height: 120,
          marginBottom: -100,
        },
        {
          image: await this.getBase64ImageFromURL(
            "assets/logos/france_transfert_logo.svg"
          ),
          width: 239,
          height: 100,
          marginLeft: 320,
        },
        {
          text: this._translate.instant('Récapitulatif_Pli'),
          fontSize: 20,
          bold: true,
          alignment: "center",
          color: "#000091",
        },
        {
          text: this._translate.instant('Informations_Pli'),
          style: "sectionHeader",
        },
        {

          columns: [
            {
              text: this._translate.instant('Type_Pli'),
              bold: true,
              margin: [0, 10, 0, 0],
            },
            this.getTypeObject(this.fileInfos.publicLink),
          ]
        },
        {
          columns: [
            this.getCheckMessage(this.fileInfos.recipientsMails, this.fileInfos.deletedRecipients, 'Nombre_Téléchargements'),
            this.getCheckObject(this.fileInfos.recipientsMails, this.fileInfos.deletedRecipients, this.fileInfos.downloadCount),
          ]
        },
        {
          columns: [
            this.getInfoMessage(this.fileInfos.senderEmail, 'Emetteur'),
            this.getInfoObject(this.fileInfos.senderEmail),
          ]
        },
        {
          columns: [
            this.getInfoMessage(this.fileInfos.subject, 'Object'),
            this.getInfoObject(this.fileInfos.subject),
          ]
        },
        {
          columns: [
            this.getInfoMessage(this.fileInfos.message, 'Message_Admin'),
            this.getInfoObject(this.fileInfos.message),
          ]
        },
        {
          columns: [
            this.getInfoMessage(this.selectedDate.toString(), 'Date_Emission'),
            this.getInfoObject(this.selectedDate.toLocaleDateString(this._translate.currentLang, {
              day: 'numeric',
              month: 'long',
              year: 'numeric',
              hour: "numeric",
              minute: "numeric",
              second: "numeric"
            })),
          ]
        },
        {
          columns: [
            {
              text: this.fileInfos.expired && this.fileInfos.archiveUntilDate != '2000-01-31' ? this._translate.instant('Date_Archive') : this._translate.instant('Date_Fin_Validité'),
              bold: true,
              margin: [0, 10, 0, 0],
            },
            {
              text: `${this.fileInfos.expired && this.fileInfos.archiveUntilDate != '2000-01-31' ?
                new Date(this.event.setHours(0, 0, 0)).toLocaleTimeString(this._translate.currentLang, {
                  day: 'numeric',
                  month: 'long',
                  year: 'numeric',
                  hour: "numeric",
                  minute: "numeric",
                  second: "numeric"
                })
                : date.toLocaleDateString(this._translate.currentLang, {
                  day: 'numeric',
                  month: 'long',
                  year: 'numeric',
                  hour: "numeric",
                  minute: "numeric",
                  second: "numeric"
                })
                }`,
              alignment: 'right',
              margin: [0, 10, 0, 0],
            },
          ]
        },
        {
          text: this.fileInfos.publicLink ? "" : this._translate.instant('Destinataires'),
          style: "sectionHeader",
        },
        this.getEducationObject(),
        {
          text: this._translate.instant('Éléments_Pli'),
          style: "sectionHeader",
        },
        {
          columns: [

            this.getInfoMessage(this.fileInfos.totalSize, 'Total_Size'),
            this.getSizeObject(this.fileInfos.totalSize),

          ]
        },

        this.fileInfos.rootFiles.length > 0 ?
          {
            text: this._translate.instant('Fichiers'),
            bold: true,
            margin: [0, 10, 0, 0],
          } : "",

        this.fileInfos.rootFiles.length > 0 ? this.getFilesObject() : "",
        this.fileInfos.rootDirs.length > 0 ?
          {
            text: this._translate.instant('Dossiers'),
            bold: true,
            margin: [0, 10, 0, 0],
          } : "",
        this.fileInfos.rootDirs.length > 0 ? this.getFoldersObject() : "",
      ],

      footer: {
        columns: [
          '',
          {
            alignment: 'right',
            text: this.formatDate(new Date())
          }
        ],
        margin: [10, 0]
      },
      styles: {
        sectionHeader: {
          bold: true,
          decoration: "underline",
          fontSize: 14,
          margin: [0, 15, 0, 15],
        },
        tableHeader: {
          bold: true,
        }
      },
    };

    if (action === "download") {
      let namePDF = this._translate.instant(typePackage);
      pdfMake.createPdf(docDefinition).download(namePDF);
    } else if (action === "print") {
      pdfMake.createPdf(docDefinition).print();
    } else {
      pdfMake.createPdf(docDefinition).open();
    }
  }

  getBase64ImageFromURL(url) {
    return new Promise((resolve, reject) => {
      var img = new Image();
      img.setAttribute("crossOrigin", "anonymous");

      img.onload = () => {
        var canvas = document.createElement("canvas");
        var ctx = canvas.getContext("2d");
        ctx.drawImage(img, 0, 0);

        var dataURL = canvas.toDataURL("image/png");

        resolve(dataURL);
      };

      img.onerror = error => {
        reject(error);
      };

      img.src = url;
    });
  }

  getInfoMessage(info: string, name: string) {
    if (info != "" && info != null) {
      return {
        text: this._translate.instant(name),
        bold: true,
        margin: [0, 10, 0, 0],
      };
    }
  }

  getCheckMessage(info1: string, info2: string, name: string) {
    if (info1.length == 0 && info2.length == 0) {
      return {
        text: this._translate.instant(name),
        bold: true,
        margin: [0, 10, 0, 0],
      };
    }
  }


  getInfoObject(info: string) {
    if (info != "" && info != null) {
      return {
        text: `${new DOMParser().parseFromString(info, 'text/html').body.textContent}`,
        alignment: 'right',
        margin: [0, 10, 0, 0],
      };
    }
  }

  getCheckObject(info1: string, info2: string, name: string) {
    if (info1.length == 0 && info2.length == 0) {
      return {
        text: `${name}`,
        alignment: 'right',
        margin: [0, 10, 0, 0],
      };
    }
  }

  getSizeObject(info: string) {
    if (info != "" && info != null) {
      const taillePli = info.split(" ");
      return {
        text: `${taillePli[0] + ' ' + this._translate.instant(taillePli[1])}`,
        alignment: 'right',
        margin: [0, 10, 0, 0],
      };
    }
  }

  getTypeObject(info: boolean) {
    if (info) {
      return {
        text: `${this._translate.instant('Lien')}`,
        alignment: 'right',
        margin: [0, 10, 0, 0],
      };
    } else {
      return {
        text: `${this._translate.instant('Courriel')}`,
        alignment: 'right',
        margin: [0, 10, 0, 0],
      };
    }
  }


  getEducationObject() {
    if (!this.fileInfos.publicLink) {
      return {
        table: {
          widths: ['44%', '21%', '34%'],
          body: [
            [{
              text: this._translate.instant('Liste_Destinataires'),
              style: 'tableHeader',
            },
            {
              text: this._translate.instant('Nb_Téléchargement'),
              style: 'tableHeader',
            },
            {
              text: this._translate.instant('Dates_Téléchargement'),
              style: 'tableHeader',
            },

            ],

            ...this.fileInfos.recipientsMails.map(ed => {

              this.downloadDate = [];
              ed.downloadDates.map(t => {

                this.downloadDate.push(new Date(t).toLocaleDateString(this._translate.currentLang, {
                  day: 'numeric',
                  month: 'long',
                  year: 'numeric',
                  hour: "numeric",
                  minute: "numeric",
                  second: "numeric"
                }))

              })

              return [ed.recipientMail, ed.numberOfDownloadPerRecipient, Object.keys(ed.downloadDates).length === 0 ? "" : this.downloadDate];
            })
          ]
        }
      };
    }
  }

  get translate(): TranslateService {
    return this._translate;
  }

  getFilesObject() {
    return {
      table: {
        widths: ['63%', '18%', '18%'],
        body: [
          [{
            text: this._translate.instant('Nom'),
            style: 'tableHeader',
          },
          {
            text: this._translate.instant('Taille'),
            style: 'tableHeader',
          },
          {
            text: this._translate.instant('Type'),
            style: 'tableHeader',
          },

          ],

          ...this.fileInfos.rootFiles.map(file => {
            this.transfer.push({ ...file, folder: false } as FTTransferModel<Transfer>);

            this.newUnit = this.pipe.transform(file.size);


            this.translate.stream(this.newUnit).subscribe(v => {
              this.unitSize = v;
            })
            return [file.name, this.sizePipe.transform(file.size) + this.unitSize, this.filePipe.transform(file.name)];
          })
        ]
      }
    };
  }


  getFoldersObject() {
    return {
      table: {
        widths: ['63%', '18%', '18%'],
        body: [
          [{
            text: 'Nom',
            style: 'tableHeader',
          },
          {
            text: this._translate.instant('Taille'),
            style: 'tableHeader',
          },
          ],


          ...this.fileInfos.rootDirs.map(file => {
            this.transferFolder.push({ ...file, size: file.totalSize, folder: true } as FTTransferModel<Transfer>)
            this.newUnit = this.pipe.transform(file.totalSize);

            this.translate.stream(this.newUnit).subscribe(v => {
              this.unitSize = v;
            })

            return [file.name, this.sizePipe.transform(file.totalSize) + this.unitSize];
          })
        ]
      }
    };
  }


  padTo2Digits(num: number) {
    return num.toString().padStart(2, '0');
  }

  formatDate(date: Date) {
    return (
      this._translate.instant('Edité') +
      [
        this.now.toLocaleDateString(this._translate.instant('Langue'))
      ]
      + ' ' + this._translate.instant('time') + ' ' +
      [
        this.padTo2Digits(date.getHours()),
        this.padTo2Digits(date.getMinutes()),
        this.padTo2Digits(date.getSeconds()),
      ].join(':')
    );
  }

}

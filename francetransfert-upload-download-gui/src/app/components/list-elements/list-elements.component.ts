/*
  * Copyright (c) Ministère de la Culture (2022)
  *
  * SPDX-License-Identifier: MIT
  * License-Filename: LICENSE.txt
  */

import { AfterViewInit, ChangeDetectorRef, Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FlowDirective, Transfer, UploadState } from '@flowjs/ngx-flow';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';
import { FileManagerService, MailingListService } from 'src/app/services';
import { ConfigService } from 'src/app/services/config/config.service';
import { MatLegacySnackBar as MatSnackBar } from "@angular/material/legacy-snack-bar";
import { InfoMsgComponent } from "../info-msg/info-msg.component";
import { TranslateService } from '@ngx-translate/core';
import { FileUnitPipe } from 'src/app/shared/pipes';

@Component({
  selector: 'ft-list-elements',
  templateUrl: './list-elements.component.html',
  styleUrls: ['./list-elements.component.scss'],
  providers: [FileUnitPipe]
})
export class ListElementsComponent implements OnInit, AfterViewInit, OnDestroy {

  @Input() component: 'upload' | 'download';
  @Input() transfers: Array<any>;
  @Input() flow: FlowDirective;
  @Input() screenWidth: string;
  @Output() listExpanded: EventEmitter<boolean> = new EventEmitter();
  filesSize: number = 0;
  fileSizeLimit: number = 1024 * 1024 * 1024 * 2;
  filesSizeLimit: number = 1024 * 1024 * 1024 * 20;
  errorMessage: string = '';
  expanded: boolean = false;
  mimetype: string[] = [];
  extension: string[] = [];
  flowAttributes: any;
  firstFile: boolean = true;
  oldLength: number = 0;
  hasError: boolean = false;
  unauthorizedFile: string;
  file: string = '';
  uploadSubscription: Subscription;
  newUnit: any;
  unitSize: string;


  constructor(private cdr: ChangeDetectorRef,
    private fileManagerService: FileManagerService,
    private configService: ConfigService,
    private translate: TranslateService,
    private _snackBar: MatSnackBar,
    private pipe: FileUnitPipe,
  ) {

    this.configService.getConfig().pipe(take(1)).subscribe((config: any) => {
      this.mimetype = config.mimeType;
      this.extension = config.extension;
      //Not used yet to limit file selection
      //this.flowAttributes = { accept: this.mimetype };
    })

  }

  ngOnInit(): void {
    if (this.component === 'download') {
      this.transfers.forEach(t => {
        this.filesSize += t.size;
      });
      this.newUnit = this.pipe.transform(this.filesSize);
      this.translate.stream(this.newUnit).pipe(take(1)).subscribe(v => {
        this.unitSize = v;
      })
    }



  }

  ngAfterViewInit() {
    this.uploadSubscription = this.flow.events$.subscribe((event) => {
      if (event.type === 'filesSubmitted') {
        this.fileManagerService.transfers.next(this.flow.transfers$);
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy() {
    this.uploadSubscription.unsubscribe();
  }

  /**
   * Returns transfer Id
   * @param {Transfer} transfer
   */
  trackTransfer(index: any, transfer): string {
    return transfer.id;
  }

  /**
   * Delete transfer
   * @param {Transfer} transfer
   * @returns {void}
   */
  deleteFolder(event) {
    for (let child of event.childs) {
      this.flow.cancelFile(child);
    }
  }

  deleteTransfer(transfer: any): void {
    if (!transfer.folder) {
      this.flow.cancelFile(transfer);
      this.filesSize -= transfer.size;
      this.fileManagerService.hasFiles.next(this.filesSize > 0);
      this.cdr.detectChanges();
      if (this.filesSize <= this.filesSizeLimit) {
        this.errorMessage = '';
        this.unauthorizedFile = '';
        this.file = '';
      }
    } else {
      for (let tr of transfer.childs) {
        this.deleteTransfer(tr);
      }
    }
    this.oldLength = this.flow.flowJs.files.length;
    if (this.flow.flowJs.files.length === 0) {
      this.firstFile = true;
    }
  }


  onItemAdded(event, index) {

    if (!this.checkExisteFile(event)) {
      if (!this.checkExtentionValid(event)) {
        if (event.folder) {
          for (let child of event.childs) {
            this.flow.cancelFile(child);
          }

        }

        this.hasError = true;
        this.cdr.detectChanges();
      } else if (event.folder) {

        try {

          this.checkSize(event, this.filesSize);
          if (index == 0) {
            this.filesSize = 0;
          }
          this.filesSize += event.size;
          this.fileManagerService.hasFiles.next(this.filesSize > 0);
          this.errorMessage = '';
          this.unauthorizedFile = '';
          this.file = '';
          this.hasError = false;
          this.cdr.detectChanges();
        } catch (error) {
          // c'est un dossier
          this.deleteFolder(event)
          if (this.filesSize < 0) {
            this.filesSize = 0;
          }
          this.fileManagerService.hasFiles.next(this.filesSize > 0);
          this.errorMessage = error.message;
          this.unauthorizedFile = '';
          this.file = '';
          this.hasError = true;
          this.cdr.detectChanges();
          return;
        }
      } else {
        if (this.filesSize <= this.filesSizeLimit && event.size <= this.fileSizeLimit) {
          if (index == 0) {
            this.filesSize = 0;
          }
          this.filesSize += event.size;
          this.fileManagerService.hasFiles.next(this.filesSize > 0);
          this.errorMessage = '';
          this.unauthorizedFile = '';
          this.file = '';
          this.hasError = false;
          this.cdr.detectChanges();
        } else {
          this.flow.cancelFile(event);
          this.errorMessage = 'TailleMaximale';
          this.unauthorizedFile = '';
          this.file = '';
          this.hasError = true;
          this.cdr.detectChanges();
        }
      }
    } else {
      if (!this.hasError) {
        this.openSnackBar(4000);
      }
    }
    if (index == this.flow.flowJs.files.length - 1) {
      this.hasError = false;
    }
    this.oldLength = this.flow.flowJs.files.length;
    this.newUnit = this.pipe.transform(this.filesSize);
    this.translate.stream(this.newUnit).subscribe(v => {
      this.unitSize = v;
    })
  }

  expandList() {
    this.expanded = !this.expanded;
    this.listExpanded.emit(this.expanded);
  }

  checkExisteFile(file) {
    let existe = false;
    //si c'est le premier fichier length egal à 1
    if (this.firstFile) {
      this.oldLength = 1;
    }
    // comparer length avec le tableau des fichiers
    //si c'est different alors le fichier n'existe pas
    if (this.oldLength == this.flow.flowJs.files.length) {
      if (this.firstFile == false) {
        existe = true;
      }
    } else {
      let seen = new Set();
      let dup = undefined;
      const hasDuplicates = this.flow.flowJs.files.some(function (file) {
        const check = seen.size === seen.add(file.relativePath).size;
        if (check) {
          dup = file;
        }
        return check;
      });
      if (hasDuplicates) {
        this.flow.flowJs.removeFile(dup);
      }
      existe = hasDuplicates;
    }
    this.firstFile = false;
    return existe;
  }


  checkExtentionValid(event: any) {
    let valid = false;
    if (event?.name) {
      if (event.folder) {
        for (let child of event.childs) {
          const fileExt = child.name.split('.').pop();
          if (this.extension.includes(fileExt)) {
            this.file = 'TypeFichier';
            this.errorMessage = 'NonAutorisé';
            this.unauthorizedFile = child.name;
            return false;
          }
        }
        valid = true;
      } else {
        const fileExt = event.name.split('.').pop();
        if (!this.extension.includes(fileExt)) {
          valid = true;
        }
        else {
          this.flow.cancelFile(event);
          this.file = '';
          this.errorMessage = 'FichierNonAutorisé';
          this.unauthorizedFile = '';
        }
      }

    }
    return valid;

  }

  checkSize(fileEvent, size) {
    let tmpSize = size;
    //Si c'est un dossier on recurse en faisant la somme des tailles des fichiers
    if (fileEvent.folder) {
      for (let child of fileEvent.childs) {
        tmpSize += this.checkSize(child, tmpSize);
        if (tmpSize > this.filesSizeLimit) {
          throw new Error('TailleMaximalePli');
        }
      }
    } else {
      // si le fichier est ok on return sa taille sinon on throw une erreur
      if (fileEvent.size > this.fileSizeLimit) {
        throw new Error('TailleMaximaleFichier');
      } else {
        return fileEvent.size;
      }
    }
    // on return la taille du dossier pour gérer le recurse
    return tmpSize;
  }

  openSnackBar(duration: number) {
    this._snackBar.openFromComponent(InfoMsgComponent, {
      panelClass: 'file-exist',
      duration: duration,
    });
  }
}


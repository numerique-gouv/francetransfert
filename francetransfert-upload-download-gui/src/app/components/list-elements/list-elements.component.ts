/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { AfterViewInit, ChangeDetectorRef, Component, OnDestroy, OnInit, inject, input, output, signal } from '@angular/core';
import { FlowConfig, Transfer, UploadState, FlowDrop, FlowButton } from '@flowjs/ngx-flow';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';
import { FileManagerService, MailingListService } from 'src/app/services';
import { ConfigService } from 'src/app/services/config/config.service';
import { MatSnackBar } from "@angular/material/snack-bar";
import { InfoMsgComponent } from "../info-msg/info-msg.component";
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { FileUnitPipe } from 'src/app/shared/pipes';
import { MatCard, MatCardHeader, MatCardTitle, MatCardContent } from '@angular/material/card';
import { DragDropFileUploadDirective } from '../../shared/directives/drag-drop-file-upload.directive';
import { NgClass, AsyncPipe } from '@angular/common';
import { MatIcon } from '@angular/material/icon';
import { FileItemComponent } from './file-item/file-item.component';
import { MatIconButton } from '@angular/material/button';
import { FileSizePipe } from '../../shared/pipes/file-size.pipe';
import { TransfersMappingPipe } from '../../shared/pipes/transfer-mapping.pipe';

@Component({
    selector: 'ft-list-elements',
    templateUrl: './list-elements.component.html',
    styleUrls: ['./list-elements.component.scss'],
    providers: [FileUnitPipe],
    imports: [MatCard, DragDropFileUploadDirective, FlowDrop, NgClass, MatCardHeader, MatCardTitle, MatIcon, MatCardContent, FileItemComponent, MatIconButton, FlowButton, AsyncPipe, TranslatePipe, FileSizePipe, TransfersMappingPipe]
})
export class ListElementsComponent implements OnInit, AfterViewInit, OnDestroy {
  private cdr = inject(ChangeDetectorRef);
  private fileManagerService = inject(FileManagerService);
  private configService = inject(ConfigService);
  private translate = inject(TranslateService);
  private _snackBar = inject(MatSnackBar);
  private pipe = inject(FileUnitPipe);


  readonly component = input<'upload' | 'download'>(undefined);
  readonly transfers = input<Array<any>>(undefined);
  readonly flow = input<FlowConfig>(undefined);
  readonly screenWidth = input<string>(undefined);
  readonly listExpanded = output<boolean>();
  readonly filesSize = signal<number>(0);
  fileSizeLimit: number = 1024 * 1024 * 1024 * 2;
  filesSizeLimit: number = 1024 * 1024 * 1024 * 20;
  maxFilenameLength: number = 150;
  readonly errorMessage = signal<string>('');
  readonly expanded = signal<boolean>(false);
  mimetype: string[] = [];
  extension: string[] = [];
  flowAttributes: any;
  firstFile: boolean = true;
  oldLength: number = 0;
  hasError: boolean = false;
  readonly unauthorizedFile = signal<string>(undefined);
  readonly file = signal<string>('');
  uploadSubscription: Subscription;
  private unitSizeSubscription: Subscription;
  newUnit: any;
  readonly unitSize = signal<string>(undefined);


  constructor() {

    this.configService.getConfig().pipe(take(1)).subscribe((config: any) => {
      this.mimetype = config.mimeType;
      this.extension = config.extension;
      //Not used yet to limit file selection
      //this.flowAttributes = { accept: this.mimetype };
    })

  }

  ngOnInit(): void {
    if (this.component() === 'download') {
      this.transfers().forEach(t => {
        this.filesSize.update(v => v + (t.size));
      });
      this.newUnit = this.pipe.transform(this.filesSize());
      this.translate.stream(this.newUnit).pipe(take(1)).subscribe(v => {
        this.unitSize.set(v);
      })
    }



  }

  ngAfterViewInit() {
    this.uploadSubscription = this.flow().events$.subscribe((event) => {
      if (event.type === 'filesSubmitted') {
        this.fileManagerService.transfers.set(this.flow().transfers$);
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy() {
    this.uploadSubscription?.unsubscribe();
    this.unitSizeSubscription?.unsubscribe();
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
    // Only cancel if event has flowFile property
    if (event.childs && event.childs.length > 0) {
      for (let child of event.childs) {
        if (child.flowFile) {
          try {
            this.flow().cancelFile(child);
          } catch (e) {
          }
        }
        this.flow().flowJs.removeFile(child);
      }
    }
    this.flow().flowJs.removeFile(event);
    // for (let child of event.childs) {
    //   this.flow().cancelFile(child);
    // }
  }

  /**
   * Recalculate total file size from the actual files array
   * This ensures accuracy by calculating from source of truth
   */
  recalculateFilesSize(): void {
    if (!this.flow() || !this.flow().flowJs || !this.flow().flowJs.files) {
      this.filesSize.set(0);
      return;
    }

    let totalSize = 0;

    totalSize = this.flow().flowJs.files.reduce((acc, file) => {
      return acc + (file as any).size || 0;
    }, 0);

    this.filesSize.set(totalSize);
  }

  deleteTransfer(transfer: any): void {
    if (!transfer.folder) {
      this.flow().cancelFile(transfer);
      this.flow().flowJs.removeFile(transfer);
    } else {
      for (let tr of transfer.childs) {
        this.deleteTransfer(tr);
      }
    }

    this.recalculateFilesSize();
    this.fileManagerService.hasFiles.set(this.filesSize() > 0);
    if (this.filesSize() <= this.filesSizeLimit) {
      this.errorMessage.set('');
      this.unauthorizedFile.set('');
      this.file.set('');
    }

    this.oldLength = this.flow().flowJs.files.length;
    if (this.flow().flowJs.files.length === 0) {
      this.firstFile = true;
    }

    this.cdr.detectChanges();
  }


  onItemAdded(event, index) {
    if (!this.checkExisteFile()) {

      if (!this.checkExtentionValid(event)) {
        if (event.folder) {
          for (let child of event.childs) {
            if (child.flowFile) {
              try {
                this.flow().cancelFile(child);
              } catch (e) {
              }
            }
          }
        }
        this.recalculateFilesSize();
        this.hasError = true;

      } else if (event.folder) {

        try {
          this.checkSize(event, this.filesSize());
          this.recalculateFilesSize();
          this.fileManagerService.hasFiles.set(this.filesSize() > 0);
          this.errorMessage.set('');
          this.unauthorizedFile.set('');
          this.file.set('');
          this.hasError = false;

          this.cdr.detectChanges();
        } catch (error) {

          // c'est un dossier
          this.deleteFolder(event)
          this.openSnackBar(4000, error.message, 'file-exist');
          if (this.filesSize() < 0) {
            this.filesSize.set(0);
          }
          this.fileManagerService.hasFiles.set(this.filesSize() > 0);
          this.errorMessage.set(error.message);
          this.unauthorizedFile.set('');
          this.file.set('');
          this.hasError = true;

          this.cdr.detectChanges();
          return;
        }
      } else {


        this.recalculateFilesSize();
        if (this.filesSize() <= this.filesSizeLimit && event.size <= this.fileSizeLimit) {

          this.fileManagerService.hasFiles.set(this.filesSize() > 0);
          this.errorMessage.set('');
          this.unauthorizedFile.set('');
          this.file.set('');
          this.hasError = false;
          this.cdr.detectChanges();

        } else {
          this.flow().cancelFile(event);
          this.errorMessage.set('TailleMaximale');
          this.unauthorizedFile.set('');
          this.file.set('');
          this.hasError = true;
          this.openSnackBar(4000, this.errorMessage(), 'file-exist');

          this.cdr.detectChanges();
        }
      }
    } else {
      // this.flow().flowJs.removeFile(event);
      if (!this.hasError) {
        this.openSnackBar(4000, 'Fichier_Dossier_DéjàPrésent', 'file-exist');
      }
    }
    if (index == this.flow().flowJs.files.length - 1) {
      this.hasError = false;
    }
    this.oldLength = this.flow().flowJs.files.length;
    this.newUnit = this.pipe.transform(this.filesSize());
    this.unitSizeSubscription?.unsubscribe();
    this.unitSizeSubscription = this.translate.stream(this.newUnit).subscribe(v => {
      this.unitSize.set(v);
    });

    this.cdr.detectChanges();
  }

  expandList() {
    this.expanded.set(!this.expanded());
    this.listExpanded.emit(this.expanded());
  }

  checkExisteFile() {
    let existe = false;
    //si c'est le premier fichier length egal à 1
    if (this.firstFile) {
      this.oldLength = 1;
    }
    // comparer length avec le tableau des fichiers
    //si c'est different alors le fichier n'existe pas
    if (this.oldLength == this.flow().flowJs.files.length) {
      if (this.firstFile == false) {
        existe = true;
      }
    } else {
      let hasDuplicates = false;
      let seen = new Set();
      let dup = [];
      this.flow().flowJs.files.forEach(file => {
        if (seen.has(file.relativePath)) {
          hasDuplicates = true;
          dup.push(file);
        } else {
          seen.add(file.relativePath);
        }
      });
      if (hasDuplicates) {
        dup.forEach(file => {
          this.flow().flowJs.removeFile(file);
        });
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
            this.file.set('TypeFichier');
            this.errorMessage.set('NonAutorisé');
            this.unauthorizedFile.set(child.name);
            this.hasError = true;
            return false;
          }
          //check file name too long
          if (child.name.length > this.maxFilenameLength
            || (child.flowFile?.file?.relativePath && child.flowFile.file.relativePath.length > this.maxFilenameLength)) {
            this.file.set('TypeFichier');
            this.errorMessage.set('NomFichierLong');
            this.hasError = true;
            this.unauthorizedFile.set(child.flowFile?.file?.relativePath ? child.flowFile.file.relativePath : child.name);
            return false;
          }
        }
        valid = true;
      } else {
        const fileExt = event.name.split('.').pop();
        if (!this.extension.includes(fileExt)) {
          valid = true;
          if (event.name.length > this.maxFilenameLength || event.flowFile?.file?.relativePath?.length > this.maxFilenameLength) {
            this.flow().cancelFile(event);
            this.file.set('TypeFichier');
            this.errorMessage.set('NomFichierLong');
            this.unauthorizedFile.set(event.flowFile?.file?.relativePath ? event.flowFile.file.relativePath : event.name);
            this.hasError = true;
            return false;
          }
        }
        else {
          this.flow().cancelFile(event);
          this.file.set('');
          this.errorMessage.set('FichierNonAutorisé');
          this.unauthorizedFile.set('');
          this.hasError = true;
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
        const childSizeResult = this.checkSize(child, tmpSize);
        tmpSize += childSizeResult;
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

  openSnackBar(duration: number, message: string, panelClass: string) {
    this._snackBar.openFromComponent(InfoMsgComponent, {
      panelClass: panelClass,
      data: {
        message: message,
      },
      duration: duration,
    });
  }
}


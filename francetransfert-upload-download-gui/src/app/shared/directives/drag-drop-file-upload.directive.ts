/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { Directive, HostListener, HostBinding, output } from '@angular/core';

@Directive({ selector: '[ftDragDropFileUpload]' })

export class DragDropFileUploadDirective {

  readonly fileDropped = output<any>();

  @HostBinding('style.background-color') protected background = '#ffffff';

  // Dragover Event
  @HostListener('dragover', ['$event']) dragOver(event) {
    event.preventDefault();
    event.stopPropagation();
    this.background = '#e2eefd';
  }

  // Dragleave Event
  @HostListener('dragleave', ['$event']) public dragLeave(event) {
    event.preventDefault();
    event.stopPropagation();
    this.background = '#ffffff'
  }

  // Drop Event
  @HostListener('drop', ['$event']) public drop(event) {
    event.preventDefault();
    event.stopPropagation();
    this.background = '#ffffff';
    const files = event.dataTransfer.files;
    if (files.length > 0) {
      this.fileDropped.emit(files)
    }
  }

}

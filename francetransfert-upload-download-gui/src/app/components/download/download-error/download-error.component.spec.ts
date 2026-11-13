/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DownloadManagerService } from 'src/app/services';
import { DownloadErrorComponent } from './download-error.component';

describe('DownloadErrorComponent', () => {
  let component: DownloadErrorComponent;
  let fixture: ComponentFixture<DownloadErrorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [DownloadErrorComponent]
})
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DownloadErrorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('maps the download error to its message key', () => {
    const manager = TestBed.inject(DownloadManagerService);
    expect(component.errorMessage()).toBe('Erreur_Récupération_Pli');

    manager.downloadError.set({ statusCode: 400, message: 'DOWNLOAD_LIMIT' });
    expect(component.errorMessage()).toBe('Nombre_Téléchargements_Dépassé');

    manager.downloadError.set({ statusCode: 400, message: 'DELETED_ENCLOSURE' });
    expect(component.errorMessage()).toBe('Existence_Pli');

    manager.downloadError.set({ statusCode: 400, message: 'HASH_INVALID' });
    expect(component.errorMessage()).toBe('Pli_Corrompu');

    manager.downloadError.set(null);
    expect(component.errorMessage()).toBe('Erreur_Récupération_Pli');
  });
});

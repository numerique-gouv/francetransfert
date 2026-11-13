/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Routes } from '@angular/router';
import { AccessibiliteComponent } from './components/accessibilite/accessibilite.component';
import { AdminComponent } from './components/admin/admin.component';
import { ContactComponent } from './components/contact/contact.component';
import { DownloadComponent } from './components/download/download.component';
import { FaqComponent } from './components/faq/faq.component';
import { MentionsLegalesComponent } from './components/mentions-legales/mentions-legales.component';
import { PolitiqueProtectionDonneesComponent } from './components/politique-protection-donnees/politique-protection-donnees.component';
import { UploadComponent } from './components/upload/upload.component';
import { CguComponent } from './components/cgu/cgu.component';
import { ConnectComponent } from './components/connect/connect.component';
import { PlisComponent } from './components/plis/plis.component';

export const routes: Routes = [
  { path: 'upload', component: UploadComponent },
  {
    path: 'download', component: DownloadComponent,
    children: [
      { path: 'download-info-public', component: DownloadComponent }
    ]
  },
  { path: 'connect', component: ConnectComponent },
  { path: 'admin', component: AdminComponent },
  { path: 'accessibilite', component: AccessibiliteComponent },
  { path: 'faq', component: FaqComponent },
  { path: 'contact', component: ContactComponent },
  { path: 'mentions-legales', component: MentionsLegalesComponent },
  { path: 'politique-de-protection-des-donnees', component: PolitiqueProtectionDonneesComponent },
  { path: 'cgu', component: CguComponent },
  { path: 'plis', component: PlisComponent },
  { path: '', redirectTo: 'upload', pathMatch: 'full' },
  { path: '**', redirectTo: 'upload' }
];

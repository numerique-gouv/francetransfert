/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MailingListManagerComponent } from './mailing-list-manager.component';

describe('MailingListManagerComponent', () => {
  let component: MailingListManagerComponent;
  let fixture: ComponentFixture<MailingListManagerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [MailingListManagerComponent]
})
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(MailingListManagerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the error for an imported file without ";"', async () => {
    const file = new File(['not-a-mailing-list'], 'list.txt', { type: 'text/plain' });
    component.loadMailingListFromFile({ target: { files: [file] } });

    await vi.waitFor(() => expect(component.errorMessage()).not.toBe(''));
    await fixture.whenStable();

    const error: HTMLElement = fixture.nativeElement.querySelector('.error[role="alert"]');
    expect(error?.textContent).toContain('Le fichier importé est invalide.');
  });
});

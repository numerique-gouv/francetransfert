/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';

import { AdminService } from 'src/app/services';
import { AdminComponent } from './admin.component';

describe('AdminComponent', () => {
  let component: AdminComponent;
  let fixture: ComponentFixture<AdminComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [AdminComponent]
})
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AdminComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });


  it('should update fileInfos with new references when a recipient is deleted', () => {
    const dialog = TestBed.inject(MatDialog);
    vi.spyOn(dialog, 'open').mockReturnValue({ afterClosed: () => of(true) } as any);
    vi.spyOn(TestBed.inject(AdminService), 'deleteRecipient').mockReturnValue(of(true));

    const alice = { recipientMail: 'alice@example.com', numberOfDownloadPerRecipient: 0 };
    const bob = { recipientMail: 'bob@example.com', numberOfDownloadPerRecipient: 0 };
    component.params = { enclosure: 'enc', token: 'tok' } as any;
    component.fileInfos.set({ recipientsMails: [alice, bob], deletedRecipients: [] });
    const before = component.fileInfos();

    component.deleteRecipient(0, alice);

    const after = component.fileInfos();
    expect(after).not.toBe(before);
    expect(after.recipientsMails).toEqual([bob]);
    expect(after.deletedRecipients).toEqual([alice]);
    // the previous value is left untouched
    expect(before.recipientsMails).toEqual([alice, bob]);
  });
});

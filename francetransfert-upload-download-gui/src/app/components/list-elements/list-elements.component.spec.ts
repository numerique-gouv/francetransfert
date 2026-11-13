/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { of } from 'rxjs';
import { ListElementsComponent } from './list-elements.component';

const flowStub: any = {
  events$: of(),
  transfers$: of({ transfers: [], totalProgress: 0 }),
  flowJs: { files: [], removeFile: () => undefined },
  cancelFile: () => undefined,
};

describe('ListElementsComponent', () => {
  let component: ListElementsComponent;
  let fixture: ComponentFixture<ListElementsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [ListElementsComponent]
})
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ListElementsComponent);
    fixture.componentRef.setInput('flow', flowStub);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

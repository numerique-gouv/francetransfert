/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LoaderService } from 'src/app/services/loader/loader.service';
import { PlisEnvoyesComponent } from './plis-envoyes.component';

describe('PlisEnvoyesComponent', () => {
  let component: PlisEnvoyesComponent;
  let fixture: ComponentFixture<PlisEnvoyesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [PlisEnvoyesComponent]
})
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PlisEnvoyesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('stops polling the export when the loader overlay is closed', () => {
    const loader = TestBed.inject(LoaderService);
    loader.show('Export en cours', undefined, true);
    fixture.detectChanges();
    component.interval = setInterval(() => undefined, 60000);
    const clear = vi.spyOn(globalThis, 'clearInterval');

    loader.hide();
    fixture.detectChanges();

    expect(clear).toHaveBeenCalledWith(component.interval);
  });
});

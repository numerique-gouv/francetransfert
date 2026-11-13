/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FileManagerService } from 'src/app/services';
import { LoaderComponent } from './loader.component';

describe('LoaderComponent', () => {
  let component: LoaderComponent;
  let fixture: ComponentFixture<LoaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [LoaderComponent]
})
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LoaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('follows the upload progress and reports the end of the transfer', () => {
    const finished = vi.fn();
    component.transferFinished.subscribe(finished);
    const fileManager = TestBed.inject(FileManagerService);

    fileManager.uploadProgress.set({ totalProgress: 0.5, transfers: [{ complete: false }] } as any);
    fixture.detectChanges();
    expect(component.progressValue()).toBe(50);
    expect(finished).not.toHaveBeenCalled();

    fileManager.uploadProgress.set({ totalProgress: 1, transfers: [{ complete: true, success: true }] } as any);
    fixture.detectChanges();
    expect(component.progressValue()).toBe(100);
    expect(finished).toHaveBeenCalledWith(true);
  });

  it('reports a failed transfer and then ignores the following upload states', () => {
    const failed = vi.fn();
    const finished = vi.fn();
    component.transferFailed.subscribe(failed);
    component.transferFinished.subscribe(finished);
    const fileManager = TestBed.inject(FileManagerService);

    fileManager.uploadProgress.set({ totalProgress: 0.4, transfers: [{ error: true, success: false, complete: false }] } as any);
    fixture.detectChanges();
    expect(failed).toHaveBeenCalledWith(true);

    fileManager.uploadProgress.set({ totalProgress: 1, transfers: [{ complete: true, success: true }] } as any);
    fixture.detectChanges();
    expect(finished).not.toHaveBeenCalled();
  });
});

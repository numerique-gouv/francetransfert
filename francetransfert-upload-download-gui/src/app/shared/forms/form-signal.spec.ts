/*
  * Copyright (c) Direction Interministérielle du Numérique
  *
  * SPDX-License-Identifier: Apache-2.0
  * License-Filename: LICENSE.txt
  */

import { Component, effect, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { formSignal } from './form-signal';

// The form sits in an embedded view, like the forms displayed behind an @if in the app.
@Component({
  imports: [ReactiveFormsModule],
  template: `
    @if (shown()) {
      <form [formGroup]="form()">
        <input formControlName="email">
        @if (form().controls.email.errors?.quota) {
          <span class="error">quota</span>
        }
      </form>
    }`,
})
class HostComponent {
  readonly shown = signal(true);
  // Invalid from the start: the errors below change while the status stays INVALID,
  // which is the case Angular does not re-render on its own.
  readonly form = formSignal(new FormGroup({ email: new FormControl('', Validators.required) }));
}

describe('formSignal', () => {
  let fixture: ComponentFixture<HostComponent>;
  let element: HTMLElement;

  beforeEach(async () => {
    fixture = TestBed.createComponent(HostComponent);
    element = fixture.nativeElement;
    await fixture.whenStable();
  });

  it('returns the form itself', () => {
    const form = new FormGroup({ email: new FormControl('a@b.fr') });

    expect(formSignal(form)()).toBe(form);
  });

  it('re-renders the view when an error is set outside of a DOM event (HTTP callback)', async () => {
    expect(element.querySelector('.error')).toBeNull();

    fixture.componentInstance.form().controls.email.setErrors({ quota: true });
    await fixture.whenStable();

    expect(element.querySelector('.error')).not.toBeNull();

    fixture.componentInstance.form().controls.email.setErrors({ notValid: true });
    await fixture.whenStable();

    expect(element.querySelector('.error')).toBeNull();
  });

  it('notifies on value, touched and reset events', () => {
    const form = formSignal(new FormGroup({ email: new FormControl('') }));
    let notifications = 0;
    TestBed.runInInjectionContext(() => effect(() => { form(); notifications++; }));
    TestBed.tick();
    expect(notifications).toBe(1);

    form().controls.email.setValue('a@b.fr');
    TestBed.tick();
    expect(notifications).toBe(2);

    form().markAllAsTouched();
    TestBed.tick();
    expect(notifications).toBe(3);

    form().reset();
    TestBed.tick();
    expect(notifications).toBe(4);
  });
});

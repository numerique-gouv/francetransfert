import { Injectable } from '@angular/core';
import { BehaviorSubject } from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class LoaderService {

  isLoading$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  loadingMessage$: BehaviorSubject<string> = new BehaviorSubject<string>('');
  downloadKeyId$: BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);
  showSpinner$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  hideSubject: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(true);


  constructor() { }

  show(message: string, downloadKeyId: string | null = null, showSpinner: boolean): void {
    this.loadingMessage$.next(message);
    this.downloadKeyId$.next(downloadKeyId);
    this.showSpinner$.next(showSpinner);
    this.isLoading$.next(true);
  }

  hide(): void {
    this.isLoading$.next(false);
    this.loadingMessage$.next('');
    this.downloadKeyId$.next(null);
    this.showSpinner$.next(null);
    this.hideSubject.next(true);
  }
}

import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LoaderService {

  readonly isLoading = signal(false);
  readonly loadingMessage = signal('');
  readonly downloadKeyId = signal<string | null>(null);
  readonly showSpinner = signal(false);

  show(message: string, downloadKeyId: string | null = null, showSpinner: boolean): void {
    this.loadingMessage.set(message);
    this.downloadKeyId.set(downloadKeyId);
    this.showSpinner.set(showSpinner);
    this.isLoading.set(true);
  }

  hide(): void {
    this.isLoading.set(false);
    this.loadingMessage.set('');
    this.downloadKeyId.set(null);
    this.showSpinner.set(false);
  }
}

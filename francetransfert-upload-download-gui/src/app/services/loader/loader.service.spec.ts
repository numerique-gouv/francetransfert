import { TestBed } from '@angular/core/testing';

import { LoaderService } from './loader.service';

describe('LoaderService', () => {
  let service: LoaderService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LoaderService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('show() displays the overlay and hide() resets it', () => {
    service.show('Export en cours', 'key-1', true);
    expect(service.isLoading()).toBe(true);
    expect(service.loadingMessage()).toBe('Export en cours');
    expect(service.downloadKeyId()).toBe('key-1');
    expect(service.showSpinner()).toBe(true);

    service.hide();
    expect(service.isLoading()).toBe(false);
    expect(service.loadingMessage()).toBe('');
    expect(service.downloadKeyId()).toBeNull();
    expect(service.showSpinner()).toBe(false);
  });
});

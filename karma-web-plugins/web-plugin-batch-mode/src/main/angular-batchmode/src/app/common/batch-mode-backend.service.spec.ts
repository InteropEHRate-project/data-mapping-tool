import { TestBed } from '@angular/core/testing';

import { BatchModeBackendService } from './batch-mode-backend.service';

describe('BatchModeBackendService', () => {
  let service: BatchModeBackendService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(BatchModeBackendService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

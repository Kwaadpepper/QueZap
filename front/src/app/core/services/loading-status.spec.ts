import { TestBed } from '@angular/core/testing'

import { LoadingStatus } from './loading-status'

describe('LoadingStatus', () => {
  let service: LoadingStatus

  beforeEach(() => {
    TestBed.configureTestingModule({})
    service = TestBed.inject(LoadingStatus)
  })

  it('should be created', () => {
    expect(service).toBeTruthy()
  })
})

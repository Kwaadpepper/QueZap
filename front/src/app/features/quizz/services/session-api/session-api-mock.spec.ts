import { TestBed } from '@angular/core/testing'

import { SessionApiMockService } from './session-api-mock'

describe('SessionApiMockService', () => {
  let service: SessionApiMockService

  beforeEach(() => {
    TestBed.configureTestingModule({})
    service = TestBed.inject(SessionApiMockService)
  })

  it('should be created', () => {
    expect(service).toBeTruthy()
  })
})

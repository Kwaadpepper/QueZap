import { TestBed } from '@angular/core/testing'

import { SessionMockService } from './session-mock'

describe('SessionMockService', () => {
  let service: SessionMockService

  beforeEach(() => {
    TestBed.configureTestingModule({})
    service = TestBed.inject(SessionMockService)
  })

  it('should be created', () => {
    expect(service).toBeTruthy()
  })
})

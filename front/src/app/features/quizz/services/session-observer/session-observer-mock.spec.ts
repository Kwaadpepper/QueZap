import { TestBed } from '@angular/core/testing'

import { SessionObserverMockService } from './session-observer-mock'

describe('SessionObserver', () => {
  let service: SessionObserverMockService

  beforeEach(() => {
    TestBed.configureTestingModule({})
    service = TestBed.inject(SessionObserverMockService)
  })

  it('should be created', () => {
    expect(service).toBeTruthy()
  })
})

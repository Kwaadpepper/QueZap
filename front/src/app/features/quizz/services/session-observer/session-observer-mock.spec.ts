import { TestBed } from '@angular/core/testing'

import { SessionMocks } from '../session.mock'

import { SessionObserverMockService } from './session-observer-mock'

describe('SessionObserver', () => {
  let service: SessionObserverMockService

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [SessionMocks, SessionObserverMockService] })
    service = TestBed.inject(SessionObserverMockService)
  })

  it('should be created', () => {
    expect(service).toBeTruthy()
  })
})

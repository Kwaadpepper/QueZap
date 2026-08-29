import { TestBed } from '@angular/core/testing'

import { SessionMocks } from '../session.mock'

import { SessionApiMockService } from './session-api-mock'

describe('SessionApiMockService', () => {
  let service: SessionApiMockService

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [SessionMocks, SessionApiMockService] })
    service = TestBed.inject(SessionApiMockService)
  })

  it('should be created', () => {
    expect(service).toBeTruthy()
  })
})

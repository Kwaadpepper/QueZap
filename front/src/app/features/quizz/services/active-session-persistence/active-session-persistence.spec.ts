import { TestBed } from '@angular/core/testing'

import { ActiveSessionPersistence } from './active-session-persistence'

describe('ActiveSessionPersistence', () => {
  let service: ActiveSessionPersistence

  beforeEach(() => {
    TestBed.configureTestingModule({})
    service = TestBed.inject(ActiveSessionPersistence)
  })

  it('should be created', () => {
    expect(service).toBeTruthy()
  })
})

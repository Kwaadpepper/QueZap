import { TestBed } from '@angular/core/testing'

import { AUTHENTICATION_SERVICE, AuthenticationService } from './authentication'

describe('AuthenticationService', () => {
  let service: AuthenticationService

  beforeEach(() => {
    TestBed.configureTestingModule({})
    service = TestBed.inject(AUTHENTICATION_SERVICE)
  })

  it('should be created', () => {
    expect(service).toBeTruthy()
  })
})

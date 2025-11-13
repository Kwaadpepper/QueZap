import { TestBed } from '@angular/core/testing'

import { AUTHENTICATION_SERVICE, AuthenticationService } from './authentication'
import { AuthenticationMockService } from './authentication-mock'

describe('AuthenticationService', () => {
  let service: AuthenticationService

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: AUTHENTICATION_SERVICE,
          useValue: new AuthenticationMockService(),
        },
      ],
    })
    service = TestBed.inject(AUTHENTICATION_SERVICE)
  })

  it('should be created', () => {
    expect(service).toBeTruthy()
  })
})

import { TestBed } from '@angular/core/testing'

import { TokenPersitance } from './token-persitance'

describe('TokenPersitance', () => {
  let service: TokenPersitance

  beforeEach(() => {
    TestBed.configureTestingModule({})
    service = TestBed.inject(TokenPersitance)
  })

  it('should be created', () => {
    expect(service).toBeTruthy()
  })
})

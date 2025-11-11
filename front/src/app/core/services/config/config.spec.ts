import { TestBed } from '@angular/core/testing'

import { Config } from './config'

describe('ConfigService', () => {
  let service: Config

  beforeEach(() => {
    TestBed.configureTestingModule({})
    service = TestBed.inject(Config)
  })

  it('should be created', () => {
    expect(service).toBeTruthy()
  })
})

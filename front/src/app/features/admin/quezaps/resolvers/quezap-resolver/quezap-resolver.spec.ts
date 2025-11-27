import { TestBed } from '@angular/core/testing'
import { ResolveFn } from '@angular/router'

import { quezapResolver } from './quezap-resolver'

describe('quezapResolver', () => {
  const executeResolver: ResolveFn<boolean> = (...resolverParameters) =>
    TestBed.runInInjectionContext(() => quezapResolver(...resolverParameters))

  beforeEach(() => {
    TestBed.configureTestingModule({})
  })

  it('should be created', () => {
    expect(executeResolver).toBeTruthy()
  })
})

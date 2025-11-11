import { HttpInterceptorFn } from '@angular/common/http'
import { TestBed } from '@angular/core/testing'

import { refreshSessionInterceptor } from './refresh-session-interceptor'

describe('refreshSessionInterceptor', () => {
  const interceptor: HttpInterceptorFn = (req, next) =>
    TestBed.runInInjectionContext(() => refreshSessionInterceptor(req, next))

  beforeEach(() => {
    TestBed.configureTestingModule({})
  })

  it('should be created', () => {
    expect(interceptor).toBeTruthy()
  })
})

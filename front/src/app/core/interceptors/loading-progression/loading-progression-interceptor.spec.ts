import { HttpInterceptorFn } from '@angular/common/http'
import { TestBed } from '@angular/core/testing'

import { loadingProgressionInterceptor } from './loading-progression-interceptor'

describe('loadingProgressionInterceptor', () => {
  const interceptor: HttpInterceptorFn = (req, next) =>
    TestBed.runInInjectionContext(() => loadingProgressionInterceptor(req, next))

  beforeEach(() => {
    TestBed.configureTestingModule({})
  })

  it('should be created', () => {
    expect(interceptor).toBeTruthy()
  })
})

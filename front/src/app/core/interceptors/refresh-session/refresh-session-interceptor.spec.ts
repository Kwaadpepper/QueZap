import {
  HttpClient,
  HttpContext,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'

import { forkJoin, of, Subject, throwError } from 'rxjs'

import { IS_REFRESHING_ACCESS_TOKEN } from '@quezap/core/tokens'
import { AuthTokens } from '@quezap/domain/models'
import { JWT } from '@quezap/domain/types'
import { AuthenticatedUserStore } from '@quezap/shared/stores/authenticated-user'

import { refreshSessionInterceptor } from './refresh-session-interceptor'

describe('refreshSessionInterceptor', () => {
  let http: HttpClient
  let httpMock: HttpTestingController
  let currentTokensSignal: ReturnType<typeof signal<AuthTokens | undefined>>
  let mockStore: {
    currentTokens: ReturnType<typeof signal<AuthTokens | undefined>>
    refresh: jest.Mock
  }

  const oldTokens: AuthTokens = {
    accessToken: 'expired-access' as JWT,
    refreshToken: 'valid-refresh' as JWT,
  }
  const newTokens: AuthTokens = {
    accessToken: 'fresh-access' as JWT,
    refreshToken: 'fresh-refresh' as JWT,
  }

  beforeEach(() => {
    currentTokensSignal = signal<AuthTokens | undefined>(undefined)
    mockStore = {
      currentTokens: currentTokensSignal,
      refresh: jest.fn(),
    }

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([refreshSessionInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthenticatedUserStore, useValue: mockStore },
      ],
    })

    http = TestBed.inject(HttpClient)
    httpMock = TestBed.inject(HttpTestingController)
  })

  afterEach(() => {
    httpMock.verify()
  })

  it('should attach Authorization header when tokens are available', () => {
    currentTokensSignal.set(oldTokens)

    http.get('/apiv1/quezaps').subscribe()

    const req = httpMock.expectOne('/apiv1/quezaps')
    expect(req.request.headers.get('Authorization')).toBe(`Bearer ${oldTokens.accessToken}`)
    req.flush([])
  })

  it('should NOT attach Authorization header or refresh on 401 for external URLs', () => {
    currentTokensSignal.set(oldTokens)
    let caughtStatus: number | undefined

    http.get('https://evil.example/apiv1/quezaps').subscribe({
      error: (err) => {
        caughtStatus = err.status
      },
    })

    const req = httpMock.expectOne('https://evil.example/apiv1/quezaps')
    expect(req.request.headers.has('Authorization')).toBe(false)
    req.flush({}, { status: 401, statusText: 'Unauthorized' })

    expect(mockStore.refresh).not.toHaveBeenCalled()
    expect(caughtStatus).toBe(401)
  })

  it('should NOT attach Authorization header for static asset URLs', () => {
    currentTokensSignal.set(oldTokens)

    http.get('/config.json').subscribe()
    const configReq = httpMock.expectOne('/config.json')
    expect(configReq.request.headers.has('Authorization')).toBe(false)
    configReq.flush({})

    http.get('images/logo.png').subscribe()
    const imageReq = httpMock.expectOne('images/logo.png')
    expect(imageReq.request.headers.has('Authorization')).toBe(false)
    imageReq.flush('ok')

    http.get('markdown/help.md').subscribe()
    const mdReq = httpMock.expectOne('markdown/help.md')
    expect(mdReq.request.headers.has('Authorization')).toBe(false)
    mdReq.flush('# Help')

    http.get('/apiv1-other/test').subscribe()
    const otherReq = httpMock.expectOne('/apiv1-other/test')
    expect(otherReq.request.headers.has('Authorization')).toBe(false)
    otherReq.flush({})
  })

  it('should not attach Authorization header or intercept 401 when IS_REFRESHING_ACCESS_TOKEN is true', () => {
    currentTokensSignal.set(oldTokens)
    const context = new HttpContext().set(IS_REFRESHING_ACCESS_TOKEN, true)
    let caughtStatus: number | undefined

    http.post('/apiv1/auth/refresh', { refreshToken: oldTokens.refreshToken }, { context }).subscribe({
      error: (err) => {
        caughtStatus = err.status
      },
    })

    const req = httpMock.expectOne('/apiv1/auth/refresh')
    expect(req.request.headers.has('Authorization')).toBe(false)
    req.flush({ message: 'Expired' }, { status: 401, statusText: 'Unauthorized' })

    expect(mockStore.refresh).not.toHaveBeenCalled()
    expect(caughtStatus).toBe(401)
  })

  it('should refresh tokens once on 401 and replay request with updated header', () => {
    currentTokensSignal.set(oldTokens)
    mockStore.refresh.mockImplementation(() => {
      currentTokensSignal.set(newTokens)
      return of(void 0)
    })

    let responseData: unknown
    http.get('/apiv1/quezaps').subscribe((res) => {
      responseData = res
    })

    const firstAttempt = httpMock.expectOne('/apiv1/quezaps')
    expect(firstAttempt.request.headers.get('Authorization')).toBe(`Bearer ${oldTokens.accessToken}`)
    firstAttempt.flush({}, { status: 401, statusText: 'Unauthorized' })

    const retryAttempt = httpMock.expectOne('/apiv1/quezaps')
    expect(retryAttempt.request.headers.get('Authorization')).toBe(`Bearer ${newTokens.accessToken}`)
    retryAttempt.flush({ ok: true })

    expect(mockStore.refresh).toHaveBeenCalledTimes(1)
    expect(responseData).toEqual({ ok: true })
  })

  it('should deduplicate concurrent 401 refreshes with shareReplay', () => {
    currentTokensSignal.set(oldTokens)
    const refreshSubject = new Subject<void>()
    mockStore.refresh.mockReturnValue(refreshSubject)

    let results: unknown[] | undefined
    forkJoin([
      http.get('/apiv1/quezaps'),
      http.get('/apiv1/sessions'),
    ]).subscribe((res) => {
      results = res
    })

    const req1 = httpMock.expectOne('/apiv1/quezaps')
    const req2 = httpMock.expectOne('/apiv1/sessions')
    req1.flush({}, { status: 401, statusText: 'Unauthorized' })
    req2.flush({}, { status: 401, statusText: 'Unauthorized' })

    expect(mockStore.refresh).toHaveBeenCalledTimes(1)

    currentTokensSignal.set(newTokens)
    refreshSubject.next(void 0)
    refreshSubject.complete()

    const retry1 = httpMock.expectOne('/apiv1/quezaps')
    const retry2 = httpMock.expectOne('/apiv1/sessions')
    expect(retry1.request.headers.get('Authorization')).toBe(`Bearer ${newTokens.accessToken}`)
    expect(retry2.request.headers.get('Authorization')).toBe(`Bearer ${newTokens.accessToken}`)

    retry1.flush({ items: ['q1'] })
    retry2.flush({ items: ['s1'] })

    expect(results).toEqual([{ items: ['q1'] }, { items: ['s1'] }])
  })

  it('should rethrow error when refresh fails', () => {
    currentTokensSignal.set(oldTokens)
    const refreshError = new Error('Refresh failed')
    mockStore.refresh.mockReturnValue(throwError(() => refreshError))

    let caughtError: unknown
    http.get('/apiv1/quezaps').subscribe({
      error: (err) => {
        caughtError = err
      },
    })

    const req = httpMock.expectOne('/apiv1/quezaps')
    req.flush({}, { status: 401, statusText: 'Unauthorized' })

    expect(mockStore.refresh).toHaveBeenCalledTimes(2) // initial + retry(1)
    expect(caughtError).toBe(refreshError)
  })
})

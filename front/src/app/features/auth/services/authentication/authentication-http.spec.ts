import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'

import { firstValueFrom, forkJoin } from 'rxjs'

import { ForbidenError, UnauthorizedError, ValidationError } from '@quezap/core/errors'
import { IS_REFRESHING_ACCESS_TOKEN } from '@quezap/core/tokens'
import { isFailure, isSuccess } from '@quezap/core/types'

import { AuthenticationHttpService } from './authentication-http'

const flushMicrotasks = () => new Promise<void>(resolve => setTimeout(resolve, 0))

describe('AuthenticationHttpService', () => {
  let service: AuthenticationHttpService
  let httpMock: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthenticationHttpService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    })

    service = TestBed.inject(AuthenticationHttpService)
    httpMock = TestBed.inject(HttpTestingController)
  })

  afterEach(() => {
    httpMock.verify()
  })

  it('should login and return accessToken on 200', async () => {
    const resultPromise = firstValueFrom(service.login('user@example.net', 'secret'))
    await flushMicrotasks()

    const req = httpMock.expectOne(r => r.url.endsWith('/apiv1/auth/login'))
    expect(req.request.method).toBe('POST')
    expect(req.request.body).toEqual({ identifier: 'user@example.net', password: 'secret' })
    req.flush({ accessToken: 'acc-123', tokenType: 'Bearer', expiresIn: 900 })

    const output = await resultPromise
    expect(isSuccess(output)).toBe(true)
    if (isSuccess(output)) {
      expect(output.result).toEqual({ accessToken: 'acc-123' })
    }
  })

  it('should return UnauthorizedError on login failure (401)', async () => {
    const resultPromise = firstValueFrom(service.login('user@example.net', 'wrong'))
    await flushMicrotasks()

    const req = httpMock.expectOne(r => r.url.endsWith('/apiv1/auth/login'))
    req.flush({ message: 'Bad credentials' }, { status: 401, statusText: 'Unauthorized' })

    const output = await resultPromise
    expect(isFailure(output)).toBe(true)
    if (isFailure(output)) {
      expect(output.error).toBeInstanceOf(UnauthorizedError)
    }
  })

  it('should set IS_REFRESHING_ACCESS_TOKEN context, send no body, and deduplicate refresh calls', async () => {
    const resultPromise = firstValueFrom(
      forkJoin([
        service.refresh(),
        service.refresh(),
      ]),
    )
    await flushMicrotasks()

    const req = httpMock.expectOne(r => r.url.endsWith('/apiv1/auth/refresh'))
    expect(req.request.method).toBe('POST')
    expect(req.request.body).toBeNull()
    expect(req.request.context.get(IS_REFRESHING_ACCESS_TOKEN)).toBe(true)

    req.flush({ accessToken: 'acc-new', tokenType: 'Bearer', expiresIn: 900 })

    const [first, second] = await resultPromise
    expect(isSuccess(first)).toBe(true)
    expect(isSuccess(second)).toBe(true)
    if (isSuccess(first) && isSuccess(second)) {
      expect(first.result).toEqual({ accessToken: 'acc-new' })
      expect(second.result).toEqual({ accessToken: 'acc-new' })
    }
  })

  it('should return UnauthorizedError when refresh fails', async () => {
    const resultPromise = firstValueFrom(service.refresh())
    await flushMicrotasks()

    const req = httpMock.expectOne(r => r.url.endsWith('/apiv1/auth/refresh'))
    expect(req.request.context.get(IS_REFRESHING_ACCESS_TOKEN)).toBe(true)
    req.flush({}, { status: 401, statusText: 'Unauthorized' })

    const output = await resultPromise
    expect(isFailure(output)).toBe(true)
    if (isFailure(output)) {
      expect(output.error).toBeInstanceOf(UnauthorizedError)
    }
  })

  it('should send { identifier } to /apiv1/auth/forgotten-password', async () => {
    const resultPromise = firstValueFrom(service.askToResetPassword('user@example.net'))
    await flushMicrotasks()

    const req = httpMock.expectOne(r => r.url.endsWith('/apiv1/auth/forgotten-password'))
    expect(req.request.method).toBe('POST')
    expect(req.request.body).toEqual({ identifier: 'user@example.net' })
    req.flush(null, { status: 202, statusText: 'Accepted' })

    const output = await resultPromise
    expect(isSuccess(output)).toBe(true)
  })

  it('should fetch authenticated user from /apiv1/auth/me', async () => {
    const resultPromise = firstValueFrom(service.me())
    await flushMicrotasks()

    const req = httpMock.expectOne(r => r.url.endsWith('/apiv1/auth/me'))
    expect(req.request.method).toBe('GET')
    req.flush({ id: 'user-1', name: 'Alice' })

    const output = await resultPromise
    expect(isSuccess(output)).toBe(true)
    if (isSuccess(output)) {
      expect(output.result).toEqual({ id: 'user-1', pseudo: 'Alice' })
    }
  })

  it('should return ForbidenError when me() fails', async () => {
    const resultPromise = firstValueFrom(service.me())
    await flushMicrotasks()

    const req = httpMock.expectOne(r => r.url.endsWith('/apiv1/auth/me'))
    req.flush({}, { status: 401, statusText: 'Unauthorized' })

    const output = await resultPromise
    expect(isFailure(output)).toBe(true)
    if (isFailure(output)) {
      expect(output.error).toBeInstanceOf(ForbidenError)
    }
  })

  it('should logout via POST without body so HttpOnly cookie is cleared', async () => {
    const resultPromise = firstValueFrom(service.logout())
    await flushMicrotasks()

    const req = httpMock.expectOne(r => r.url.endsWith('/apiv1/auth/logout'))
    expect(req.request.method).toBe('POST')
    expect(req.request.body).toBeNull()
    req.flush(null, { status: 204, statusText: 'No Content' })

    const output = await resultPromise
    expect(isSuccess(output)).toBe(true)
  })

  it('should verify reset token via POST body and reset password via SDK endpoints', async () => {
    const verifyPromise = firstValueFrom(service.verifyResetToken('opaque-token'))
    await flushMicrotasks()
    const verifyReq = httpMock.expectOne(r => r.url.endsWith('/apiv1/auth/verify-reset-token'))
    expect(verifyReq.request.method).toBe('POST')
    expect(verifyReq.request.body).toEqual({ token: 'opaque-token' })
    verifyReq.flush(null, { status: 200, statusText: 'OK' })
    expect(isSuccess(await verifyPromise)).toBe(true)

    const resetPromise = firstValueFrom(service.resetPassword('opaque-token', 'NewPassword1!'))
    await flushMicrotasks()
    const resetReq = httpMock.expectOne(r => r.url.endsWith('/apiv1/auth/reset-password'))
    expect(resetReq.request.method).toBe('POST')
    expect(resetReq.request.body).toEqual({ token: 'opaque-token', newPassword: 'NewPassword1!' })
    resetReq.flush({ message: 'OK' })
    expect(isSuccess(await resetPromise)).toBe(true)

    const resetFailPromise = firstValueFrom(service.resetPassword('bad', 'short'))
    await flushMicrotasks()
    const resetFailReq = httpMock.expectOne(r => r.url.endsWith('/apiv1/auth/reset-password'))
    resetFailReq.flush({}, { status: 400, statusText: 'Bad Request' })
    const resetFailOutput = await resetFailPromise
    expect(isFailure(resetFailOutput)).toBe(true)
    if (isFailure(resetFailOutput)) {
      expect(resetFailOutput.error).toBeInstanceOf(ValidationError)
    }
  })
})

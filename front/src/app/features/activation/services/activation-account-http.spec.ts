import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'

import { firstValueFrom } from 'rxjs'

import { ValidationError } from '@quezap/core/errors'
import { isFailure, isSuccess } from '@quezap/core/types'

import { AccountActivationHttpService } from './activation-account-http'

const flushMicrotasks = () => new Promise<void>(resolve => setTimeout(resolve, 0))

describe('AccountActivationHttpService', () => {
  let service: AccountActivationHttpService
  let httpMock: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AccountActivationHttpService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    })

    service = TestBed.inject(AccountActivationHttpService)
    httpMock = TestBed.inject(HttpTestingController)
  })

  afterEach(() => {
    httpMock.verify()
  })

  it('should activate account via SDK activate endpoint', async () => {
    const resultPromise = firstValueFrom(service.activate('activation-token-123'))
    await flushMicrotasks()

    const req = httpMock.expectOne(r => r.url.endsWith('/apiv1/auth/activate'))
    expect(req.request.method).toBe('POST')
    expect(req.request.body).toEqual({ token: 'activation-token-123' })
    req.flush({ message: 'Account activated' })

    const output = await resultPromise
    expect(isSuccess(output)).toBe(true)
  })

  it('should return ValidationError when activation fails', async () => {
    const resultPromise = firstValueFrom(service.activate('expired-token'))
    await flushMicrotasks()

    const req = httpMock.expectOne(r => r.url.endsWith('/apiv1/auth/activate'))
    req.flush({ message: 'Invalid token' }, { status: 400, statusText: 'Bad Request' })

    const output = await resultPromise
    expect(isFailure(output)).toBe(true)
    if (isFailure(output)) {
      expect(output.error).toBeInstanceOf(ValidationError)
    }
  })
})

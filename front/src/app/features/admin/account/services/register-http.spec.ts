import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'

import { firstValueFrom } from 'rxjs'

import { ValidationError } from '@quezap/core/errors'
import { isFailure, isSuccess } from '@quezap/core/types'

import { RegisterHttpService } from './register-http'

const flushMicrotasks = () => new Promise<void>(resolve => setTimeout(resolve, 0))

describe('RegisterHttpService', () => {
  let service: RegisterHttpService
  let httpMock: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RegisterHttpService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    })

    service = TestBed.inject(RegisterHttpService)
    httpMock = TestBed.inject(HttpTestingController)
  })

  afterEach(() => {
    httpMock.verify()
  })

  it('should create user via SDK create endpoint', async () => {
    const resultPromise = firstValueFrom(service.register('teacher@quezap.com'))
    await flushMicrotasks()

    const req = httpMock.expectOne(r => r.url.endsWith('/apiv1/users'))
    expect(req.request.method).toBe('POST')
    expect(req.request.body).toEqual({
      name: 'teacher',
      identifier: 'teacher@quezap.com',
      email: 'teacher@quezap.com',
      password: expect.any(String),
    })
    req.flush({ id: 'user-uuid' }, { status: 201, statusText: 'Created' })

    const output = await resultPromise
    expect(isSuccess(output)).toBe(true)
  })

  it('should return ValidationError on email field when registration fails', async () => {
    const resultPromise = firstValueFrom(service.register('duplicate@quezap.com'))
    await flushMicrotasks()

    const req = httpMock.expectOne(r => r.url.endsWith('/apiv1/users'))
    req.flush({ message: 'Email already used' }, { status: 409, statusText: 'Conflict' })

    const output = await resultPromise
    expect(isFailure(output)).toBe(true)
    if (isFailure(output)) {
      expect(output.error).toBeInstanceOf(ValidationError)
    }
  })
})

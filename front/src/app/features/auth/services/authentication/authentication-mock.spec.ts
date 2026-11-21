import { HttpErrorResponse } from '@angular/common/http'
import { TestBed } from '@angular/core/testing'

import { tap } from 'rxjs'

import { ValidationError } from '@quezap/core/errors'
import { isSuccess } from '@quezap/core/types'

import { TokenPersitance } from '../token-persistance/token-persitance'

import { AuthenticationMockService } from './authentication-mock'

describe('AuthenticationMockService', () => {
  const validJwtToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG'
    + '4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp - QV30'

  const createService = ({ mockError = false } = {}): AuthenticationMockService => {
    const service = TestBed.inject(AuthenticationMockService)
    Object.defineProperty(service, 'MOCK_ERROR', { value: () => mockError })
    Object.defineProperty(service, 'MOCK_DELAY', { value: () => 0 })

    return service
  }

  beforeEach(() => {
    jest.useFakeTimers()
    TestBed.configureTestingModule({
      providers: [
        AuthenticationMockService,
        { provide: TokenPersitance, useValue: { getTokens: jest.fn(() => ({ accessToken: validJwtToken })) } },
      ],
    })
  })

  afterEach(() => {
    jest.useRealTimers()
  })

  it('should be created', () => {
    const service = createService()

    expect(service).toBeTruthy()
  })

  // --- LOGIN TESTS ---

  it('should login successfully with valid credentials', () => {
    // GIVEN
    const email = 'user@example.net'
    const password = 'password'
    const service = createService()
    expect.assertions(2)

    // WHEN / THEN
    service.login(email, password)
      .pipe(
        tap((result) => {
          const isSuccessResult = isSuccess(result)
          expect(isSuccessResult).toBeTruthy()
          if (isSuccessResult) {
            expect(result.result.accessToken).toBeDefined()
          }
        }),
      ).subscribe()
    jest.runAllTimers()
  })

  it('should output validation error on login with invalid credentials', () => {
    // GIVEN
    const email = 'invalid@example.net'
    const password = 'wrong'
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.login(email, password)
      .pipe(
        tap(result => expect(result).toBeInstanceOf(ValidationError)),
      ).subscribe()
    jest.runAllTimers()
  })

  it('should output network error on login when network fails', () => {
    // GIVEN
    const email = 'user@example.net'
    const password = 'password'
    const service = createService({ mockError: true })
    expect.assertions(1)

    // WHEN / THEN
    service.login(email, password).subscribe({
      error: (error) => {
        expect(error).toBeInstanceOf(HttpErrorResponse)
      },
    })
    jest.runAllTimers()
  })

  // --- REFRESH TESTS ---

  it('should refresh tokens successfully when valid access token is present', () => {
    // GIVEN
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.refresh()
      .pipe(
        tap(result => expect(isSuccess(result)).toBeTruthy()),
      ).subscribe()
    jest.runAllTimers()
  })

  it('should output validation error on refresh when jwt validation fails', () => {
    // GIVEN
    const tokenPersistance = TestBed.inject(TokenPersitance)
    jest.spyOn(tokenPersistance, 'getTokens').mockReturnValue({ accessToken: 'invalid' as never })
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.refresh()
      .pipe(
        tap(result => expect(result).toBeInstanceOf(ValidationError)),
      ).subscribe()
    jest.runAllTimers()
  })

  it('should output network error on refresh when network fails', () => {
    // GIVEN
    const service = createService({ mockError: true })
    expect.assertions(1)

    // WHEN / THEN
    service.refresh().subscribe({
      error: (error) => {
        expect(error).toBeInstanceOf(HttpErrorResponse)
      },
    })
    jest.runAllTimers()
  })

  // --- LOGOUT TESTS ---

  it('should logout successfully', () => {
    // GIVEN
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.logout()
      .pipe(
        tap(result => expect(isSuccess(result)).toBeTruthy()),
      ).subscribe()
    jest.runAllTimers()
  })

  it('should output network error on logout when network fails', () => {
    // GIVEN
    const service = createService({ mockError: true })
    expect.assertions(1)

    // WHEN / THEN
    service.logout().subscribe({
      error: (error) => {
        expect(error).toBeInstanceOf(HttpErrorResponse)
      },
    })
    jest.runAllTimers()
  })

  // --- ME TESTS ---

  it('should return user on me', () => {
    // GIVEN
    const service = createService()
    expect.assertions(2)

    // WHEN / THEN
    service.me()
      .pipe(
        tap((result) => {
          expect(isSuccess(result)).toBeTruthy()
          if (isSuccess(result)) {
            expect(result.result.pseudo).toBeDefined()
          }
        }),
      ).subscribe()
    jest.runAllTimers()
  })

  it('should output network error on me when network fails', () => {
    // GIVEN
    const service = createService({ mockError: true })
    expect.assertions(1)

    // WHEN / THEN
    service.me().subscribe({
      error: (error) => {
        expect(error).toBeInstanceOf(HttpErrorResponse)
      },
    })
    jest.runAllTimers()
  })

  // --- ASK RESET PASSWORD TESTS ---

  it('should not output error on askToResetPassword with valid email', () => {
    // GIVEN
    const email = 'user@example.net'
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.askToResetPassword(email)
      .pipe(
        tap(result => expect(isSuccess(result)).toBeTruthy()),
      ).subscribe()
    jest.runAllTimers()
  })

  it('should output validation error on askToResetPassword with invalid email', () => {
    // GIVEN
    const email = 'invalid@example.net'
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.askToResetPassword(email)
      .pipe(
        tap(result => expect(result).toBeInstanceOf(ValidationError)),
      ).subscribe()
    jest.runAllTimers()
  })

  it('should output network error on askToResetPassword when network fails', () => {
    // GIVEN
    const email = 'user@example.net'
    const service = createService({ mockError: true })
    expect.assertions(1)

    // WHEN / THEN
    service.askToResetPassword(email).subscribe({
      error: (error) => {
        expect(error).toBeInstanceOf(HttpErrorResponse)
      },
    })
    jest.runAllTimers()
  })

  // --- VERIFY RESET TOKEN TESTS ---

  it('should not ouput error on verifyResetToken with valid token', () => {
    // GIVEN
    const tokenToVerify = validJwtToken
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.verifyResetToken(tokenToVerify)
      .pipe(
        tap(result => expect(isSuccess(result)).toBeTruthy()),
      ).subscribe()
    jest.runAllTimers()
  })

  it('should output forbiden error on verifyResetToken with invalid token', () => {
    // GIVEN
    const tokenToVerify = 'invalid-token'
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.verifyResetToken(tokenToVerify)
      .pipe(
        tap(result => expect(result).toBeInstanceOf(ValidationError)),
      ).subscribe()
    jest.runAllTimers()
  })

  it('should output network error on verifyResetToken when network fails', () => {
    // GIVEN
    const tokenToVerify = validJwtToken
    const service = createService({ mockError: true })
    expect.assertions(1)

    // WHEN / THEN
    service.verifyResetToken(tokenToVerify).subscribe({
      error: (error) => {
        expect(error).toBeInstanceOf(HttpErrorResponse)
      },
    })
    jest.runAllTimers()
  })

  // --- RESET PASSWORD TESTS ---

  it('should not ouput error on resetPassword with valid token and password', () => {
    // GIVEN
    const tokenToVerify = validJwtToken
    const newPassword = 'newStrongPassword123!'
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.resetPassword(tokenToVerify, newPassword)
      .pipe(
        tap(result => expect(isSuccess(result)).toBeTruthy()),
      ).subscribe()
    jest.runAllTimers()
  })

  it('should output forbiden error on resetPassword with invalid token', () => {
    // GIVEN
    const tokenToVerify = 'invalid-token'
    const newPassword = 'newStrongPassword123!'
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.resetPassword(tokenToVerify, newPassword)
      .pipe(
        tap(result => expect(result).toBeInstanceOf(ValidationError)),
      ).subscribe()
    jest.runAllTimers()
  })

  it('should output validation error on resetPassword with weak password', () => {
    // GIVEN
    const tokenToVerify = validJwtToken
    const newPassword = 'weak'
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.resetPassword(tokenToVerify, newPassword)
      .pipe(
        tap(result => expect(result).toBeInstanceOf(ValidationError)),
      ).subscribe()
    jest.runAllTimers()
  })

  it('should output network error on resetPassword when network fails', () => {
    // GIVEN
    const tokenToVerify = validJwtToken
    const newPassword = 'newStrongPassword123!'
    const service = createService({ mockError: true })
    expect.assertions(1)

    // WHEN / THEN
    service.resetPassword(tokenToVerify, newPassword).subscribe({
      error: (error) => {
        expect(error).toBeInstanceOf(HttpErrorResponse)
      },
    })
    jest.runAllTimers()
  })
})

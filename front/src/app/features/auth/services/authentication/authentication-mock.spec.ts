import { HttpErrorResponse } from '@angular/common/http'
import { fakeAsync, TestBed } from '@angular/core/testing'

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
    TestBed.configureTestingModule({
      providers: [
        AuthenticationMockService,
        {
          provide: TokenPersitance, useValue: {
            getTokens: jest.fn(() => ({
              accessToken: validJwtToken,
              refreshToken: validJwtToken,
            })),
          },
        },
      ],
    })
  })

  it('should be created', () => {
    const service = createService()

    expect(service).toBeTruthy()
  })

  // --- LOGIN TESTS ---

  it('should login successfully with valid credentials', fakeAsync(() => {
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
  }))

  it('should output validation error on login with invalid credentials', fakeAsync(() => {
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
  }))

  it('should output network error on login when network fails', fakeAsync(() => {
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
  }))

  // --- REFRESH TESTS ---

  it('should refresh tokens successfully with valid refresh token', fakeAsync(() => {
    // GIVEN
    const tokens = { accessToken: validJwtToken, refreshToken: validJwtToken }
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.refresh(tokens)
      .pipe(
        tap(result => expect(isSuccess(result)).toBeTruthy()),
      ).subscribe()
  }))

  it('should output validation error on refresh when jwt generation fails validation', fakeAsync(() => {
    // GIVEN
    const tokens = { accessToken: 'invalid', refreshToken: 'invalid' }
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.refresh(tokens)
      .pipe(
        tap(result => expect(result).toBeInstanceOf(ValidationError)),
      ).subscribe()
  }))

  it('should output network error on refresh when network fails', fakeAsync(() => {
    // GIVEN
    const tokens = { accessToken: validJwtToken, refreshToken: validJwtToken }
    const service = createService({ mockError: true })
    expect.assertions(1)

    // WHEN / THEN
    service.refresh(tokens).subscribe({
      error: (error) => {
        expect(error).toBeInstanceOf(HttpErrorResponse)
      },
    })
  }))

  // --- LOGOUT TESTS ---

  it('should logout successfully', fakeAsync(() => {
    // GIVEN
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.logout()
      .pipe(
        tap(result => expect(isSuccess(result)).toBeTruthy()),
      ).subscribe()
  }))

  it('should output network error on logout when network fails', fakeAsync(() => {
    // GIVEN
    const service = createService({ mockError: true })
    expect.assertions(1)

    // WHEN / THEN
    service.logout().subscribe({
      error: (error) => {
        expect(error).toBeInstanceOf(HttpErrorResponse)
      },
    })
  }))

  // --- ME TESTS ---

  it('should return user on me', fakeAsync(() => {
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
  }))

  it('should output network error on me when network fails', fakeAsync(() => {
    // GIVEN
    const service = createService({ mockError: true })
    expect.assertions(1)

    // WHEN / THEN
    service.me().subscribe({
      error: (error) => {
        expect(error).toBeInstanceOf(HttpErrorResponse)
      },
    })
  }))

  // --- ASK RESET PASSWORD TESTS ---

  it('should not output error on askToResetPassword with valid email', fakeAsync(() => {
    // GIVEN
    const email = 'user@example.net'
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.askToResetPassword(email)
      .pipe(
        tap(result => expect(isSuccess(result)).toBeTruthy()),
      ).subscribe()
  }))

  it('should output validation error on askToResetPassword with invalid email', fakeAsync(() => {
    // GIVEN
    const email = 'invalid@example.net'
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.askToResetPassword(email)
      .pipe(
        tap(result => expect(result).toBeInstanceOf(ValidationError)),
      ).subscribe()
  }))

  it('should output network error on askToResetPassword when network fails', fakeAsync(() => {
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
  }))

  // --- VERIFY RESET TOKEN TESTS ---

  it('should not ouput error on verifyResetToken with valid token', fakeAsync(() => {
    // GIVEN
    const tokenToVerify = validJwtToken
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.verifyResetToken(tokenToVerify)
      .pipe(
        tap(result => expect(isSuccess(result)).toBeTruthy()),
      ).subscribe()
  }))

  it('should output forbiden error on verifyResetToken with invalid token', fakeAsync(() => {
    // GIVEN
    const tokenToVerify = 'invalid-token'
    const service = createService()
    expect.assertions(1)

    // WHEN / THEN
    service.verifyResetToken(tokenToVerify)
      .pipe(
        tap(result => expect(result).toBeInstanceOf(ValidationError)),
      ).subscribe()
  }))

  it('should output network error on verifyResetToken when network fails', fakeAsync(() => {
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
  }))

  // --- RESET PASSWORD TESTS ---

  it('should not ouput error on resetPassword with valid token and password', fakeAsync(() => {
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
  }))

  it('should output forbiden error on resetPassword with invalid token', fakeAsync(() => {
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
  }))

  it('should output validation error on resetPassword with weak password', fakeAsync(() => {
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
  }))

  it('should output network error on resetPassword when network fails', fakeAsync(() => {
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
  }))
})

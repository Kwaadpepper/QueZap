import { signal } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { Router } from '@angular/router'

import { of } from 'rxjs'

import { ForbidenError } from '@quezap/core/errors'

import { AUTHENTICATION_SERVICE, AuthenticationService } from './../../services/authentication/authentication'
import { ResetPassword } from './reset-password'

describe('ResetPassword', () => {
  let component: ResetPassword
  let fixture: ComponentFixture<ResetPassword>

  const validJwtToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG'
    + '4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp - QV30'

  const queryTokenValue = signal<string | null>(null)

  const router = {
    currentNavigation: jest.fn().mockReturnValue({
      extractedUrl: {
        queryParamMap: {
          get: jest.fn(() => queryTokenValue()),
        },
      },
    }),
  }

  const verifyResetSeviceFails = signal(false)
  const authService: Pick<AuthenticationService, 'verifyResetToken'> = {
    verifyResetToken: jest.fn(() => {
      if (verifyResetSeviceFails()) {
        return of({
          kind: 'failure',
          error: new ForbidenError('Token verification failed'),
        })
      }
      return of({ kind: 'success', result: void 0 })
    }),
  }

  const createComponent = () => {
    fixture = TestBed.createComponent(ResetPassword)
    component = fixture.componentInstance
    fixture.detectChanges()
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResetPassword],
      providers: [
        {
          provide: Router,
          useValue: router,
        },
        {
          provide: AUTHENTICATION_SERVICE,
          useValue: authService,
        },
      ],
    })
      .compileComponents()

    queryTokenValue.set(null)
    verifyResetSeviceFails.set(false)

    createComponent()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should verify that it has a valid jwt reset token onload', () => {
    // GIVEN
    queryTokenValue.set(validJwtToken)

    // WHEN
    createComponent()

    // THEN
    expect(component).toBeTruthy()
    expect(component['tokenIsInvalid']()).toBe(false)
  })

  it('should verify the token using the service', () => {
    // GIVEN
    queryTokenValue.set(validJwtToken)

    // WHEN
    createComponent()

    // THEN
    expect(authService.verifyResetToken).toHaveBeenCalledWith(validJwtToken)
  })

  it('should show a message if the token is invalid', () => {
    // GIVEN
    queryTokenValue.set('invalid-token')

    // WHEN
    createComponent()

    // THEN
    expect(component['tokenIsInvalid']()).toBe(true)
  })

  it('should not call the service if the token is invalid', () => {
    // GIVEN
    queryTokenValue.set('invalid-token')

    // WHEN
    createComponent()

    // THEN
    expect(authService.verifyResetToken).not.toHaveBeenCalled()
  })

  it('should show an error message if the token verification fails', async () => {
    // GIVEN
    queryTokenValue.set(validJwtToken)
    verifyResetSeviceFails.set(true)

    // WHEN
    createComponent()
    await fixture.whenStable()

    // THEN
    expect(component['tokenIsInvalid']()).toBe(true)
  })

  it('should not display the form if the token is invalid', () => {
    // GIVEN
    queryTokenValue.set('invalid-token')

    // WHEN
    createComponent()

    // THEN
    expect(component['displayForm']()).toBe(false)
  })

  it('should not display the form if an error occurs during token verification', () => {
    // GIVEN
    queryTokenValue.set(validJwtToken)
    verifyResetSeviceFails.set(true)

    // WHEN
    createComponent()

    // THEN
    expect(component['displayForm']()).toBe(false)
  })

  it('should display the form when token is valid', () => {
    // GIVEN
    queryTokenValue.set(validJwtToken)

    // WHEN
    createComponent()

    // THEN
    expect(component['displayForm']()).toBe(true)
  })
})

import { signal } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { Router } from '@angular/router'

import { MessageService } from 'primeng/api'
import { of } from 'rxjs'

import { ServiceError, ValidationError } from '@quezap/core/errors'
import { ServiceOutput } from '@quezap/core/types'

import { AUTHENTICATION_SERVICE, AuthenticationService } from '../../services'

import { ResetPasswordForm } from './reset-password-form'

describe('ResetPasswordForm', () => {
  let component: ResetPasswordForm
  let fixture: ComponentFixture<ResetPasswordForm>

  const passwordIsInvalid = signal(false)
  const serviceError = signal(false)

  const validJwtToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG'
    + '4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp - QV30'

  const router = {
    navigateByUrl: jest.fn<void, [string]>(),
  }

  const message$ = {
    add: jest.fn<void, [unknown]>(),
  }

  const authService: Pick<AuthenticationService, 'resetPassword'> = {
    resetPassword: jest.fn<ServiceOutput<void>, [string, string]>(() => {
      if (serviceError()) {
        return of({
          kind: 'failure',
          error: new ServiceError('Service error'),
        })
      }
      if (passwordIsInvalid()) {
        return of({
          kind: 'failure',
          error: new ValidationError({ password: [''] }, 'Invalid password'),
        })
      }
      return of({ kind: 'success', result: void 0 })
    }),
  }

  beforeEach(async () => {
    jest.clearAllMocks()

    await TestBed.configureTestingModule({
      imports: [ResetPasswordForm],
      providers: [
        { provide: Router, useValue: router },
        { provide: MessageService, useValue: message$ },
        { provide: AUTHENTICATION_SERVICE, useValue: authService },
      ],
    })
      .compileComponents()

    fixture = TestBed.createComponent(ResetPasswordForm)
    component = fixture.componentInstance
    component['resetValues'].set({
      password: '',
      confirmPassword: '',
    })

    fixture.componentRef.setInput('resetToken', validJwtToken)

    fixture.detectChanges()

    passwordIsInvalid.set(false)
    serviceError.set(false)
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should submit the new password when form is submitted', async () => {
    // GIVEN
    const newPassword = 'NewPassword123!'
    component['resetValues'].set({
      password: newPassword,
      confirmPassword: newPassword,
    })
    fixture.detectChanges()

    // WHEN
    component['onReset']()
    await fixture.whenStable()

    // THEN
    expect(authService.resetPassword).toHaveBeenCalledWith(
      validJwtToken,
      newPassword,
    )
  })

  it('should display an error message if password reset fails', async () => {
    // GIVEN
    serviceError.set(true)
    const newPassword = 'weak'
    component['resetValues'].set({
      password: newPassword,
      confirmPassword: newPassword,
    })
    fixture.detectChanges()

    // WHEN
    component['onReset']()
    await fixture.whenStable()

    // THEN
    expect(authService.resetPassword).toHaveBeenCalled()
    expect(component['anErrorOccured']()).toBeTruthy()
  })

  it('should display input errors if the form is invalid', async () => {
    // GIVEN
    const newPassword = 'NewPassword123!'
    component['resetValues'].set({
      password: newPassword,
      confirmPassword: 'DifferentPassword!',
    })
    fixture.detectChanges()

    // WHEN
    component['onReset']()
    await fixture.whenStable()

    // THEN
    expect(authService.resetPassword).not.toHaveBeenCalled()
    expect(component['resetForm'].confirmPassword().errors()).toHaveLength(1)
  })

  it('should redirect if password reset is successful', async () => {
    // GIVEN
    const newPassword = 'NewPassword123!'
    component['resetValues'].set({
      password: newPassword,
      confirmPassword: newPassword,
    })
    fixture.detectChanges()

    // WHEN
    component['onReset']()
    await fixture.whenStable()

    // THEN
    expect(authService.resetPassword).toHaveBeenCalled()
    expect(router.navigateByUrl).toHaveBeenCalled()
  })
})

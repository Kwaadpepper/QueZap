import { signal } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { ActivatedRoute } from '@angular/router'

import { MessageService } from '@openng/optimus-ui/api'

import { of } from 'rxjs'

import { ForbidenError } from '@quezap/core/errors'

import { AUTHENTICATION_SERVICE, AuthenticationService } from './../../services/authentication/authentication'
import { ResetPassword } from './reset-password'

describe('ResetPassword', () => {
  let component: ResetPassword
  let fixture: ComponentFixture<ResetPassword>

  const validJwtToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG'
    + '4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp - QV30'
  const opaqueResetToken = '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d'

  const queryTokenValue = signal<string | null>(null)

  const activatedRoute = { snapshot: { queryParamMap: { get: jest.fn(() => queryTokenValue()) } } }

  const message$ = { add: jest.fn<void, [unknown]>() }

  const verifyResetSeviceFails = signal(false)
  const auth$: Pick<AuthenticationService, 'verifyResetToken'> = {
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
        { provide: ActivatedRoute, useValue: activatedRoute },
        { provide: AUTHENTICATION_SERVICE, useValue: auth$ },
        { provide: MessageService, useValue: message$ },
      ],
    })
      .compileComponents()

    queryTokenValue.set(null)
    verifyResetSeviceFails.set(false)
    jest.clearAllMocks()

    createComponent()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should verify a JWT reset token on load', () => {
    queryTokenValue.set(validJwtToken)

    createComponent()

    expect(component['tokenIsInvalid']()).toBe(false)
    expect(auth$.verifyResetToken).toHaveBeenCalledWith(validJwtToken)
  })

  it('should accept and verify an opaque non-JWT reset token', () => {
    queryTokenValue.set(opaqueResetToken)

    createComponent()

    expect(component['tokenIsInvalid']()).toBe(false)
    expect(component['displayForm']()).toBe(true)
    expect(auth$.verifyResetToken).toHaveBeenCalledWith(opaqueResetToken)
  })

  it('should show a message and stop spinner if the token is missing or blank', () => {
    queryTokenValue.set('   ')

    createComponent()

    expect(component['tokenIsInvalid']()).toBe(true)
    expect(component['verifyingToken']()).toBe(false)
    expect(component['displayForm']()).toBe(false)
    expect(auth$.verifyResetToken).not.toHaveBeenCalled()
  })

  it('should show an error message if the token verification fails', async () => {
    queryTokenValue.set(opaqueResetToken)
    verifyResetSeviceFails.set(true)

    createComponent()
    await fixture.whenStable()

    expect(component['tokenIsInvalid']()).toBe(true)
    expect(component['displayForm']()).toBe(false)
  })

  it('should display the form when token is valid', () => {
    queryTokenValue.set(validJwtToken)

    createComponent()

    expect(component['displayForm']()).toBe(true)
  })
})

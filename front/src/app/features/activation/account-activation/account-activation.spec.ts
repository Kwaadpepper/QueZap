import { signal } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { ActivatedRoute, Router } from '@angular/router'

import { MessageService } from 'primeng/api'
import { of } from 'rxjs'

import { ValidationError } from '@quezap/core/errors'

import { ACCOUNT_ACTIVATION_SERVICE, AccountActivationService } from '../services'

import { AccountActivation } from './account-activation'

describe('AccountActivation', () => {
  let component: AccountActivation
  let fixture: ComponentFixture<AccountActivation>

  const queryTokenValue = signal<string | null>(null)
  const activatedRoute = { snapshot: { queryParamMap: { get: jest.fn(() => queryTokenValue()) } } }
  const router = { navigateByUrl: jest.fn().mockResolvedValue(true) }
  const messageService = { add: jest.fn() }
  const activateMock = jest.fn<ReturnType<AccountActivationService['activate']>, [string]>(() =>
    of({ kind: 'success', result: void 0 }),
  )
  const activationService: AccountActivationService = { activate: activateMock }

  const createComponent = async () => {
    fixture = TestBed.createComponent(AccountActivation)
    component = fixture.componentInstance
    fixture.detectChanges()
    await fixture.whenStable()
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountActivation],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRoute },
        { provide: Router, useValue: router },
        { provide: MessageService, useValue: messageService },
        { provide: ACCOUNT_ACTIVATION_SERVICE, useValue: activationService },
      ],
    })
      .compileComponents()

    queryTokenValue.set(null)
    jest.clearAllMocks()
  })

  it('should create and set errorOccurred when token is missing', async () => {
    await createComponent()
    expect(component).toBeTruthy()
    expect(component['errorOccurred']()).toBe(true)
    expect(router.navigateByUrl).toHaveBeenCalledWith('/')
  })

  it('should activate account when valid token is provided in queryParamMap', async () => {
    queryTokenValue.set('valid-activation-token')
    await createComponent()
    expect(activationService.activate).toHaveBeenCalledWith('valid-activation-token')
    expect(router.navigateByUrl).toHaveBeenCalledWith('/auth/login')
  })

  it('should set errorOccurred when activation returns ValidationError', async () => {
    queryTokenValue.set('invalid-token')
    jest.spyOn(activationService, 'activate').mockReturnValueOnce(
      of({ kind: 'failure', error: new ValidationError({}, 'Invalid token') }),
    )
    await createComponent()
    expect(component['errorOccurred']()).toBe(true)
    expect(router.navigateByUrl).toHaveBeenCalledWith('/')
  })
})

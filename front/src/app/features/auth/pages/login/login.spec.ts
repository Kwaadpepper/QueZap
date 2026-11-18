import { ErrorHandler, signal } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'

import { MessageService } from 'primeng/api'
import { of, throwError } from 'rxjs'

import { UnauthorizedError, ValidationError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services/config/config'
import { AuthenticatedUserStore } from '@quezap/shared/stores/authenticated-user'

import { Login } from './login'

describe('Login', () => {
  let component: Login
  let fixture: ComponentFixture<Login>
  let router: Router

  const mockConfig = { debug: signal(false) }
  const mockAuthStore = { login: jest.fn() }
  const mockMessageService = { add: jest.fn() }
  const mockErrorHandler = { handleError: jest.fn() }

  beforeEach(async () => {
    jest.clearAllMocks()

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([]),
        { provide: Config, useValue: mockConfig },
        { provide: AuthenticatedUserStore, useValue: mockAuthStore },
        { provide: MessageService, useValue: mockMessageService },
        { provide: ErrorHandler, useValue: mockErrorHandler },
      ],
    })
      .compileComponents()

    router = TestBed.inject(Router)
    jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true)

    fixture = TestBed.createComponent(Login)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create with correct reset link spelling and enabled submit button when not submitting', () => {
    expect(component).toBeTruthy()
    const el = fixture.nativeElement as HTMLElement
    const resetLink = el.querySelector('a[href="/auth/reset"]')
    expect(resetLink?.textContent).toContain('réinitialiser mon mot de passe')

    const submitButton = el.querySelector('button[type="submit"]') as HTMLButtonElement | null
    expect(submitButton?.disabled).toBe(false)
  })

  it('should display invalidCredentials message without global error toast on UnauthorizedError', async () => {
    mockAuthStore.login.mockReturnValue(throwError(() => new UnauthorizedError('Identifiants invalides')))
    component['loginInfo'].set({ email: 'user@example.net', password: 'wrong' })
    fixture.detectChanges()

    component['onLogin']()
    await fixture.whenStable()
    fixture.detectChanges()

    expect(component['invalidCredentials']()).toBe(true)
    expect(component['loginError']()).toBe(false)
    expect(mockErrorHandler.handleError).not.toHaveBeenCalled()
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Email ou mot de passe invalide')
    expect(component['loginInfo']().password).toBe('wrong')
  })

  it('should display invalidCredentials on ValidationError', async () => {
    mockAuthStore.login.mockReturnValue(
      throwError(() => new ValidationError({ email: ['Email invalide'] }, 'Invalid')),
    )
    component['loginInfo'].set({ email: 'user@example.net', password: 'wrong' })
    fixture.detectChanges()

    component['onLogin']()
    await fixture.whenStable()

    expect(component['invalidCredentials']()).toBe(true)
    expect(mockErrorHandler.handleError).not.toHaveBeenCalled()
  })

  it('should reset password and notify errorHandler on unexpected failure', async () => {
    mockAuthStore.login.mockReturnValue(throwError(() => new Error('Server down')))
    component['loginInfo'].set({ email: 'user@example.net', password: 'secret' })
    fixture.detectChanges()

    component['onLogin']()
    await fixture.whenStable()

    expect(component['loginError']()).toBe(true)
    expect(component['invalidCredentials']()).toBe(false)
    expect(component['loginInfo']().password).toBe('')
    expect(mockErrorHandler.handleError).toHaveBeenCalledTimes(1)
  })

  it('should navigate to /admin and show success toast when login succeeds', async () => {
    mockAuthStore.login.mockReturnValue(of(void 0))
    component['loginInfo'].set({ email: 'user@example.net', password: 'password' })
    fixture.detectChanges()

    component['onLogin']()
    await fixture.whenStable()

    expect(mockAuthStore.login).toHaveBeenCalledWith('user@example.net', 'password')
    expect(mockMessageService.add).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
    expect(router.navigateByUrl).toHaveBeenCalledWith('/admin')
  })
})

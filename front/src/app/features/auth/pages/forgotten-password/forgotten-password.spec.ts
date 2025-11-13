import { signal } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideNoopAnimations } from '@angular/platform-browser/animations'

import { of, throwError } from 'rxjs'

import { Config } from '@quezap/core/services'

import { AUTHENTICATION_SERVICE } from '../../services'

import { ForgottenPassword } from './forgotten-password'

describe('ForgottenPassword', () => {
  let component: ForgottenPassword
  let fixture: ComponentFixture<ForgottenPassword>
  const resetPipe = of(undefined)

  const mockConfig = {
    debug: signal(false),
  }

  const mockAuthenticationService = {
    resetPassword: jest.fn().mockReturnValue(resetPipe),
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForgottenPassword], // Utilisation du composant standalone
      providers: [
        // Fourniture des Mocks
        { provide: Config, useValue: mockConfig },
        { provide: AUTHENTICATION_SERVICE, useValue: mockAuthenticationService },
        // Nécessaire pour les animations des composants PrimeNG
        provideNoopAnimations(),
      ],
    }).compileComponents()

    fixture = TestBed.createComponent(ForgottenPassword)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  // Réinitialisation des Mocks après chaque test
  afterEach(() => {
    jest.clearAllMocks()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  describe('Reset form tests', () => {
    it('should init as invalid', () => {
      expect(component['resetForm']().invalid()).toBeTruthy()
    })

    it('should not call the service if form has not been validated', () => {
      // GIVEN
      component['resetInfo'].set({ email: 'invalid' })

      // WHEN
      component['onAskToReset']()

      // THEN
      expect(component['resetForm']().touched()).toBeTruthy()
      expect(mockAuthenticationService.resetPassword).not.toHaveBeenCalled()
    })

    it('should validate the form with a correct email', () => {
      // GIVEN
      const validEmail = 'user@example.net'
      component['resetInfo'].set({ email: validEmail })

      // WHEN
      fixture.detectChanges()

      // THEN
      expect(component['resetForm']().valid()).toBeTruthy()
      expect(component['resetForm'].email().value()).toBe(validEmail)
    })

    it('should mark the form as touched when trying to submit an invalid form', () => {
      // GIVEN
      component['resetInfo'].set({ email: 'invalid-email' })

      // WHEN
      component['onAskToReset']()

      // THEN
      expect(component['resetForm']().touched()).toBeTruthy()
    })

    it ('should reset the form inputs', () => {
      // GIVEN
      component['resetInfo'].set({ email: 'user@example.net' })

      // WHEN
      component['resetFormInput']()

      // THEN
      expect(component['resetInfo']().email).toBe('')
    })
  })

  describe('onAskToReset tests', () => {
    it('should submit the form and call resetPassword on the service', async () => {
      // GIVEN
      const validEmail = 'user@example.net'
      component['resetInfo'].set({ email: validEmail })

      // WHEN
      component['onAskToReset']()

      // THEN
      expect(mockAuthenticationService.resetPassword).toHaveBeenCalledWith(validEmail)
    })

    it('should show a success message upon successful password reset request', async () => {
      // GIVEN
      const validEmail = 'user@example.net'
      component['resetInfo'].set({ email: validEmail })
      fixture.detectChanges()

      // WHEN
      component['onAskToReset']()

      // Attendre que l'observable se complète
      await fixture.whenStable()

      // THEN
      expect(mockAuthenticationService.resetPassword).toHaveBeenCalledWith(validEmail)
      expect(component['hasBeenAskedToReset']()).toBeTruthy()
      expect(component['errorHasOccured']()).toBeFalsy()
    })

    it('should reset signals before submitting', () => {
      // GIVEN
      const validEmail = 'user@example.net'
      component['resetInfo'].set({ email: validEmail })
      component['hasBeenAskedToReset'].set(true)
      component['errorHasOccured'].set(true)

      // WHEN
      component['onAskToReset']()

      // THEN - Les signaux doivent être réinitialisés immédiatement
      expect(component['hasBeenAskedToReset']()).toBeFalsy()
      expect(component['errorHasOccured']()).toBeFalsy()
    })

    it('should show an error message if the resetPassword call fails', async () => {
      // GIVEN
      const validEmail = 'invalid@example.net'
      component['resetInfo'].set({ email: validEmail })
      const error = new Error('Reset failed')

      // Mock pour retourner un Observable qui rejette
      mockAuthenticationService.resetPassword.mockReturnValueOnce(
        throwError(() => error),
      )

      // WHEN
      component['onAskToReset']()
      await fixture.whenStable()

      // THEN
      expect(component['errorHasOccured']()).toBeTruthy()
      expect(component['resetInfo']().email).toBe('')
    })
  })
})

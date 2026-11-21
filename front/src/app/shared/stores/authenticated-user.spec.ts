import { TestBed } from '@angular/core/testing'

import { firstValueFrom, of, throwError } from 'rxjs'

import { ForbidenError, ServiceError, UnauthorizedError } from '@quezap/core/errors'
import { AuthenticatedUser, AuthTokens, UserId } from '@quezap/domain/models'
import { JWT } from '@quezap/domain/types'
import {
  AUTHENTICATION_SERVICE,
  AuthenticationService,
  TokenPersitance,
} from '@quezap/features/auth/services'

import { AuthenticatedUserStore } from './authenticated-user'

describe('AuthenticatedUserStore', () => {
  const initialTokens: AuthTokens = { accessToken: 'access-token-1' as JWT }
  const refreshedTokens: AuthTokens = { accessToken: 'access-token-2' as JWT }
  const mockUser: AuthenticatedUser = {
    id: '123e4567-e89b-12d3-a456-426614174000' as UserId,
    pseudo: 'Alice',
  }

  let tokenPersistanceMock: jest.Mocked<Pick<TokenPersitance, 'getTokens' | 'saveTokens' | 'removeTokens'>>
  let authServiceMock: jest.Mocked<AuthenticationService>

  const setupStore = () => {
    localStorage.clear()
    tokenPersistanceMock = {
      getTokens: jest.fn(),
      saveTokens: jest.fn(),
      removeTokens: jest.fn(),
    }

    authServiceMock = {
      login: jest.fn(),
      refresh: jest.fn(),
      logout: jest.fn(),
      me: jest.fn(),
      askToResetPassword: jest.fn(),
      verifyResetToken: jest.fn(),
      resetPassword: jest.fn(),
    }

    TestBed.configureTestingModule({
      providers: [
        AuthenticatedUserStore,
        { provide: TokenPersitance, useValue: tokenPersistanceMock },
        { provide: AUTHENTICATION_SERVICE, useValue: authServiceMock },
      ],
    })

    return TestBed.inject(AuthenticatedUserStore)
  }

  it('should initialize in an unauthenticated state', () => {
    const store = setupStore()

    expect(store.isLoggedIn()).toBe(false)
    expect(store.currentTokens()).toBeUndefined()
    expect(store.currentUser()).toBeUndefined()
    expect(store.authenticating()).toBe(false)
    expect(store.sessionExpired()).toBe(false)
  })

  describe('loadInitialState', () => {
    it('should skip refresh when no session hint is present and remain guest silently', async () => {
      const store = setupStore()
      tokenPersistanceMock.getTokens.mockReturnValue(undefined)

      await expect(firstValueFrom(store.loadInitialState())).resolves.toBeUndefined()

      expect(authServiceMock.refresh).not.toHaveBeenCalled()
      expect(authServiceMock.me).not.toHaveBeenCalled()
      expect(store.isLoggedIn()).toBe(false)
      expect(store.currentTokens()).toBeUndefined()
      expect(store.sessionExpired()).toBe(false)
    })

    it('should attempt silent refresh when session hint is present and clear hint silently on failure', async () => {
      const store = setupStore()
      localStorage.setItem('quezap_has_session', '1')
      tokenPersistanceMock.getTokens.mockReturnValue(undefined)
      authServiceMock.refresh.mockReturnValue(
        of({ kind: 'failure', error: new UnauthorizedError('No cookie') }),
      )

      await expect(firstValueFrom(store.loadInitialState())).resolves.toBeUndefined()

      expect(authServiceMock.refresh).toHaveBeenCalledTimes(1)
      expect(authServiceMock.me).not.toHaveBeenCalled()
      expect(tokenPersistanceMock.removeTokens).toHaveBeenCalledTimes(1)
      expect(localStorage.getItem('quezap_has_session')).toBeNull()
      expect(store.isLoggedIn()).toBe(false)
      expect(store.currentTokens()).toBeUndefined()
      expect(store.sessionExpired()).toBe(false)
    })

    it('should restore session silently on startup when session hint, refresh cookie, and me() succeed', async () => {
      const store = setupStore()
      localStorage.setItem('quezap_has_session', '1')
      tokenPersistanceMock.getTokens
        .mockReturnValueOnce(undefined)
        .mockReturnValue(refreshedTokens)
      authServiceMock.refresh.mockReturnValue(of({ kind: 'success', result: refreshedTokens }))
      authServiceMock.me.mockReturnValue(of({ kind: 'success', result: mockUser }))

      await firstValueFrom(store.loadInitialState())

      expect(authServiceMock.refresh).toHaveBeenCalledTimes(1)
      expect(tokenPersistanceMock.saveTokens).toHaveBeenCalledWith(refreshedTokens)
      expect(authServiceMock.me).toHaveBeenCalledTimes(1)
      expect(store.isLoggedIn()).toBe(true)
      expect(store.currentUser()).toEqual(mockUser)
      expect(store.currentTokens()).toEqual(refreshedTokens)
      expect(store.authenticating()).toBe(false)
      expect(store.sessionExpired()).toBe(false)
    })

    it('should authenticate user when persisted tokens and me() succeed', async () => {
      const store = setupStore()
      tokenPersistanceMock.getTokens.mockReturnValue(initialTokens)
      authServiceMock.me.mockReturnValue(of({ kind: 'success', result: mockUser }))

      await firstValueFrom(store.loadInitialState())

      expect(authServiceMock.me).toHaveBeenCalledTimes(1)
      expect(store.isLoggedIn()).toBe(true)
      expect(store.currentUser()).toEqual(mockUser)
      expect(store.currentTokens()).toEqual(initialTokens)
      expect(store.authenticating()).toBe(false)
      expect(store.sessionExpired()).toBe(false)
    })

    it('should preserve refreshed tokens if me() triggered a token refresh during boot', async () => {
      const store = setupStore()
      tokenPersistanceMock.getTokens
        .mockReturnValueOnce(initialTokens)
        .mockReturnValue(refreshedTokens)
      authServiceMock.refresh.mockReturnValue(of({ kind: 'success', result: refreshedTokens }))
      authServiceMock.me.mockImplementation(() => {
        store.refresh().subscribe()
        return of({ kind: 'success', result: mockUser })
      })

      await firstValueFrom(store.loadInitialState())

      expect(store.isLoggedIn()).toBe(true)
      expect(store.currentTokens()).toEqual(refreshedTokens)
      expect(store.currentUser()).toEqual(mockUser)
    })

    it('should purge expired tokens silently without throwing when me() returns failure', async () => {
      const store = setupStore()
      tokenPersistanceMock.getTokens.mockReturnValue(initialTokens)
      authServiceMock.me.mockReturnValue(
        of({ kind: 'failure', error: new ForbidenError('Expired') }),
      )

      await expect(firstValueFrom(store.loadInitialState())).resolves.toBeUndefined()

      expect(tokenPersistanceMock.removeTokens).toHaveBeenCalledTimes(1)
      expect(store.isLoggedIn()).toBe(false)
      expect(store.currentTokens()).toBeUndefined()
      expect(store.authenticating()).toBe(false)
      expect(store.sessionExpired()).toBe(true)
    })

    it('should purge tokens silently without throwing when me() errors', async () => {
      const store = setupStore()
      tokenPersistanceMock.getTokens.mockReturnValue(initialTokens)
      authServiceMock.me.mockReturnValue(throwError(() => new Error('Network down')))

      await expect(firstValueFrom(store.loadInitialState())).resolves.toBeUndefined()

      expect(tokenPersistanceMock.removeTokens).toHaveBeenCalledTimes(1)
      expect(store.isLoggedIn()).toBe(false)
      expect(store.currentTokens()).toBeUndefined()
      expect(store.sessionExpired()).toBe(true)
    })
  })

  describe('login', () => {
    it('should login, save tokens, load profile, and update authenticated state', async () => {
      const store = setupStore()
      authServiceMock.login.mockReturnValue(of({ kind: 'success', result: initialTokens }))
      authServiceMock.me.mockReturnValue(of({ kind: 'success', result: mockUser }))

      await firstValueFrom(store.login('alice@example.net', 'secret'))

      expect(authServiceMock.login).toHaveBeenCalledWith('alice@example.net', 'secret')
      expect(tokenPersistanceMock.saveTokens).toHaveBeenCalledWith(initialTokens)
      expect(authServiceMock.me).toHaveBeenCalledTimes(1)
      expect(store.isLoggedIn()).toBe(true)
      expect(store.currentUser()).toEqual(mockUser)
      expect(store.currentTokens()).toEqual(initialTokens)
      expect(store.authenticating()).toBe(false)
      expect(store.sessionExpired()).toBe(false)
    })

    it('should clean up and rethrow when login returns failure', async () => {
      const store = setupStore()
      const unauthorized = new UnauthorizedError('Invalid credentials')
      authServiceMock.login.mockReturnValue(of({ kind: 'failure', error: unauthorized }))

      await expect(firstValueFrom(store.login('alice@example.net', 'wrong'))).rejects.toThrow(unauthorized)

      expect(tokenPersistanceMock.removeTokens).toHaveBeenCalledTimes(1)
      expect(store.isLoggedIn()).toBe(false)
      expect(store.authenticating()).toBe(false)
    })

    it('should clean up and rethrow when me() fails after login', async () => {
      const store = setupStore()
      const forbidden = new ForbidenError('User disabled')
      authServiceMock.login.mockReturnValue(of({ kind: 'success', result: initialTokens }))
      authServiceMock.me.mockReturnValue(of({ kind: 'failure', error: forbidden }))

      await expect(firstValueFrom(store.login('alice@example.net', 'secret'))).rejects.toThrow(forbidden)

      expect(tokenPersistanceMock.saveTokens).toHaveBeenCalledWith(initialTokens)
      expect(tokenPersistanceMock.removeTokens).toHaveBeenCalledTimes(1)
      expect(store.isLoggedIn()).toBe(false)
    })
  })

  describe('logout', () => {
    it('should call logout and clear state on success', async () => {
      const store = setupStore()
      authServiceMock.login.mockReturnValue(of({ kind: 'success', result: initialTokens }))
      authServiceMock.me.mockReturnValue(of({ kind: 'success', result: mockUser }))
      authServiceMock.logout.mockReturnValue(of({ kind: 'success', result: undefined }))
      const logSpy = jest.spyOn(console, 'log').mockImplementation(() => undefined)

      await firstValueFrom(store.login('alice@example.net', 'secret'))
      await firstValueFrom(store.logout())

      expect(authServiceMock.logout).toHaveBeenCalledWith()
      expect(tokenPersistanceMock.removeTokens).toHaveBeenCalled()
      expect(store.isLoggedIn()).toBe(false)
      expect(store.currentUser()).toBeUndefined()
      logSpy.mockRestore()
    })

    it('should force local reset on logout failure', async () => {
      const store = setupStore()
      const serviceError = new ServiceError('Logout error')
      authServiceMock.logout.mockReturnValue(of({ kind: 'failure', error: serviceError }))
      const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined)

      await expect(firstValueFrom(store.logout())).rejects.toThrow(serviceError)

      expect(authServiceMock.logout).toHaveBeenCalledWith()
      expect(tokenPersistanceMock.removeTokens).toHaveBeenCalledTimes(1)
      expect(store.isLoggedIn()).toBe(false)
      errorSpy.mockRestore()
    })
  })

  describe('refresh', () => {
    it('should throw when neither _authenticated nor _temporaryTokens is present', async () => {
      const store = setupStore()

      await expect(firstValueFrom(store.refresh())).rejects.toThrow(
        'No authenticated user to refresh tokens for.',
      )
      expect(authServiceMock.refresh).not.toHaveBeenCalled()
    })

    it('should refresh tokens for an already authenticated user', async () => {
      const store = setupStore()
      authServiceMock.login.mockReturnValue(of({ kind: 'success', result: initialTokens }))
      authServiceMock.me.mockReturnValue(of({ kind: 'success', result: mockUser }))
      authServiceMock.refresh.mockReturnValue(of({ kind: 'success', result: refreshedTokens }))

      await firstValueFrom(store.login('alice@example.net', 'secret'))
      await firstValueFrom(store.refresh())

      expect(authServiceMock.refresh).toHaveBeenCalledWith()
      expect(tokenPersistanceMock.saveTokens).toHaveBeenCalledWith(refreshedTokens)
      expect(store.currentTokens()).toEqual(refreshedTokens)
      expect(store.isLoggedIn()).toBe(true)
      expect(store.sessionExpired()).toBe(false)
    })

    it('should refresh _temporaryTokens during boot before _authenticated is set', async () => {
      const store = setupStore()
      tokenPersistanceMock.getTokens.mockReturnValue(initialTokens)
      authServiceMock.refresh.mockReturnValue(of({ kind: 'success', result: refreshedTokens }))
      let tokensDuringMeAfterRefresh: AuthTokens | undefined
      authServiceMock.me.mockImplementation(() => {
        expect(store.currentTokens()).toEqual(initialTokens)
        store.refresh().subscribe()
        tokensDuringMeAfterRefresh = store.currentTokens()
        return of({ kind: 'success', result: mockUser })
      })

      await firstValueFrom(store.loadInitialState())

      expect(authServiceMock.refresh).toHaveBeenCalledWith()
      expect(tokensDuringMeAfterRefresh).toEqual(refreshedTokens)
      expect(store.currentTokens()).toEqual(refreshedTokens)
      expect(store.isLoggedIn()).toBe(true)
    })

    it('should clear tokens and set sessionExpired when refresh returns failure', async () => {
      const store = setupStore()
      const unauthorized = new UnauthorizedError('Refresh expired')
      authServiceMock.login.mockReturnValue(of({ kind: 'success', result: initialTokens }))
      authServiceMock.me.mockReturnValue(of({ kind: 'success', result: mockUser }))
      authServiceMock.refresh.mockReturnValue(of({ kind: 'failure', error: unauthorized }))
      const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined)

      await firstValueFrom(store.login('alice@example.net', 'secret'))
      await expect(firstValueFrom(store.refresh())).rejects.toThrow(unauthorized)

      expect(tokenPersistanceMock.removeTokens).toHaveBeenCalled()
      expect(store.isLoggedIn()).toBe(false)
      expect(store.currentTokens()).toBeUndefined()
      expect(store.sessionExpired()).toBe(true)
      errorSpy.mockRestore()
    })
  })
})

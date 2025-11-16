import { TestBed } from '@angular/core/testing'
import { ActivatedRouteSnapshot, CanActivateChildFn, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router'

import { AuthenticatedUserStore } from '@quezap/shared/stores'

import { isAuthenticatedChildGuard, isAuthenticatedGuard } from './authenticated-guard'

const mockAuthenticatedUserStore = {
  isLoggedIn: jest.fn(),
}

const mockRouter = {
  parseUrl: jest.fn(url => url),
}

describe('Authenticated Function Guards', () => {
  beforeEach(() => {
    mockAuthenticatedUserStore.isLoggedIn.mockClear()
    mockRouter.parseUrl.mockClear()

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthenticatedUserStore, useValue: mockAuthenticatedUserStore },
        { provide: Router, useValue: mockRouter },
      ],
    })
  })

  const runGuardInContext = (guardFn: CanActivateFn | CanActivateChildFn) => {
    return TestBed.runInInjectionContext(() => {
      return guardFn(
        null as unknown as ActivatedRouteSnapshot,
        null as unknown as RouterStateSnapshot,
      )
    })
  }

  describe('If the user is authenticated', () => {
    beforeEach(() => {
      mockAuthenticatedUserStore.isLoggedIn.mockReturnValue(true)
    })

    it('isAuthenticatedGuard should return TRUE', () => {
      const result = runGuardInContext(isAuthenticatedGuard)

      expect(result).toBe(true)
    })

    it('isAuthenticatedChildGuard should return TRUE', () => {
      const result = runGuardInContext(isAuthenticatedChildGuard)

      expect(result).toBe(true)
    })
  })

  describe('If the user is NOT authenticated', () => {
    beforeEach(() => {
      mockAuthenticatedUserStore.isLoggedIn.mockReturnValue(false)
    })

    it('isAuthenticatedGuard should return the redirection URL', () => {
      const expectedUrl = '/auth/login'
      mockRouter.parseUrl.mockReturnValue(expectedUrl)

      const result = runGuardInContext(isAuthenticatedGuard)

      expect(result).toBe(expectedUrl)
      expect(mockRouter.parseUrl).toHaveBeenCalledWith(expectedUrl)
    })

    it('isAuthenticatedChildGuard should return the redirection URL', () => {
      const expectedUrl = '/auth/login'
      mockRouter.parseUrl.mockReturnValue(expectedUrl)

      const result = runGuardInContext(isAuthenticatedChildGuard)

      expect(result).toBe(expectedUrl)
      expect(mockRouter.parseUrl).toHaveBeenCalledWith(expectedUrl)
    })
  })
})

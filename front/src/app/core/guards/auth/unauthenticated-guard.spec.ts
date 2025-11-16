import { TestBed } from '@angular/core/testing'
import { ActivatedRouteSnapshot, CanActivateChildFn, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router'

import { AuthenticatedUserStore } from '@quezap/shared/stores'

import { isUnauthenticatedChildGuard, isUnauthenticatedGuard } from './unauthenticated-guard'

const mockAuthenticatedUserStore = {
  isLoggedIn: jest.fn(),
}

const mockRouter = {
  parseUrl: jest.fn(url => url),
}

describe('Unauthenticated Function Guards', () => {
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

  describe('If the user is unauthenticated', () => {
    beforeEach(() => {
      mockAuthenticatedUserStore.isLoggedIn.mockReturnValue(false)
    })

    it('isUnauthenticatedGuard should return TRUE', () => {
      const result = runGuardInContext(isUnauthenticatedGuard)

      expect(result).toBe(true)
    })

    it('isUnauthenticatedChildGuard should return TRUE', () => {
      const result = runGuardInContext(isUnauthenticatedChildGuard)

      expect(result).toBe(true)
    })
  })

  describe('If the user is authenticated', () => {
    beforeEach(() => {
      mockAuthenticatedUserStore.isLoggedIn.mockReturnValue(true)
    })

    it('isUnauthenticatedGuard should return the redirection URL', () => {
      const expectedUrl = '/admin'
      mockRouter.parseUrl.mockReturnValue(expectedUrl)

      const result = runGuardInContext(isUnauthenticatedGuard)

      expect(result).toBe(expectedUrl)
      expect(mockRouter.parseUrl).toHaveBeenCalledWith(expectedUrl)
    })

    it('isUnauthenticatedChildGuard should return the redirection URL', () => {
      const expectedUrl = '/admin'
      mockRouter.parseUrl.mockReturnValue(expectedUrl)

      const result = runGuardInContext(isUnauthenticatedChildGuard)

      expect(result).toBe(expectedUrl)
      expect(mockRouter.parseUrl).toHaveBeenCalledWith(expectedUrl)
    })
  })
})

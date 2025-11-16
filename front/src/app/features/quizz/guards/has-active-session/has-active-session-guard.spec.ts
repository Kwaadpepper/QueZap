import { TestBed } from '@angular/core/testing'
import { ActivatedRouteSnapshot, CanActivateChildFn, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router'

import { ActiveSessionStore } from '../../stores'

import { hasActiveSessionChildGuard, hasActiveSessionGuard } from './has-active-session-guard'

const mockActiveSessionStore = {
  session: jest.fn(),
}

const mockRouter = {
  parseUrl: jest.fn(url => url),
}

describe('Has active session Function Guards', () => {
  beforeEach(() => {
    mockActiveSessionStore.session.mockClear()
    mockRouter.parseUrl.mockClear()

    TestBed.configureTestingModule({
      providers: [
        { provide: ActiveSessionStore, useValue: mockActiveSessionStore },
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

  describe('If an active session exists', () => {
    beforeEach(() => {
      mockActiveSessionStore.session.mockReturnValue({})
    })

    it('hasActiveSessionGuard should return TRUE', () => {
      const result = runGuardInContext(hasActiveSessionGuard)

      expect(result).toBe(true)
    })

    it('hasActiveSessionChildGuard should return TRUE', () => {
      const result = runGuardInContext(hasActiveSessionChildGuard)

      expect(result).toBe(true)
    })
  })

  describe('If there is no active session', () => {
    beforeEach(() => {
      mockActiveSessionStore.session.mockReturnValue(void 0)
    })

    it('hasActiveSessiondGuard should return the redirection URL', () => {
      const expectedUrl = '/quizz/expired'
      mockRouter.parseUrl.mockReturnValue(expectedUrl)

      const result = runGuardInContext(hasActiveSessionGuard)

      expect(result).toBe(expectedUrl)
      expect(mockRouter.parseUrl).toHaveBeenCalledWith(expectedUrl)
    })

    it('hasActiveSessionChildGuard should return the redirection URL', () => {
      const expectedUrl = '/quizz/expired'
      mockRouter.parseUrl.mockReturnValue(expectedUrl)

      const result = runGuardInContext(hasActiveSessionChildGuard)

      expect(result).toBe(expectedUrl)
      expect(mockRouter.parseUrl).toHaveBeenCalledWith(expectedUrl)
    })
  })
})

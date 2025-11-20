import { signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import {
  ActivatedRouteSnapshot, CanActivateChildFn, CanActivateFn, GuardResult, Router, RouterStateSnapshot,
} from '@angular/router'

import { firstValueFrom, Observable } from 'rxjs'

import { ActiveSessionStore } from '../../stores'

import { hasActiveSessionChildGuard, hasActiveSessionGuard } from './has-active-session-guard'

const mockActiveSessionStore = {
  session: jest.fn(),
  restorationComplete: signal(false),
}

const mockRouter = { parseUrl: jest.fn(url => url) }

describe('Has active session Function Guards', () => {
  beforeEach(() => {
    mockActiveSessionStore.session.mockClear()
    mockActiveSessionStore.restorationComplete.set(false)
    mockRouter.parseUrl.mockClear()

    TestBed.configureTestingModule({
      providers: [
        { provide: ActiveSessionStore, useValue: mockActiveSessionStore },
        { provide: Router, useValue: mockRouter },
      ],
    })
  })

  const runGuardInContext = (guardFn: CanActivateFn | CanActivateChildFn): Observable<GuardResult> => {
    return TestBed.runInInjectionContext(() => {
      return guardFn(
        null as unknown as ActivatedRouteSnapshot,
        null as unknown as RouterStateSnapshot,
      )
    }) as Observable<GuardResult>
  }

  describe('If an active session exists', () => {
    beforeEach(() => {
      mockActiveSessionStore.session.mockReturnValue(true)
      mockActiveSessionStore.restorationComplete.set(true)
    })

    it('hasActiveSessionGuard should return TRUE', async () => {
      const observable = runGuardInContext(hasActiveSessionGuard)
      const test = firstValueFrom(observable)

      await expect(test).resolves.toEqual(true)
    })

    it('hasActiveSessionChildGuard should return TRUE', async () => {
      const observable = runGuardInContext(hasActiveSessionChildGuard)
      const test = firstValueFrom(observable)

      await expect(test).resolves.toEqual(true)
    })
  })

  describe('If there is no active session', () => {
    beforeEach(() => {
      mockActiveSessionStore.session.mockReturnValue(void 0)
      mockActiveSessionStore.restorationComplete.set(true)
    })

    it('hasActiveSessionGuard should return the redirection URL', async () => {
      const expectedUrl = '/quizz/expired'
      mockRouter.parseUrl.mockReturnValue(expectedUrl)

      const observable = runGuardInContext(hasActiveSessionGuard)
      const test = firstValueFrom(observable)

      await expect(test).resolves.toEqual(expectedUrl)
      expect(mockRouter.parseUrl).toHaveBeenCalledWith(expectedUrl)
    })

    it('hasActiveSessionChildGuard should return the redirection URL', async () => {
      const expectedUrl = '/quizz/expired'
      mockRouter.parseUrl.mockReturnValue(expectedUrl)

      const observable = runGuardInContext(hasActiveSessionChildGuard)
      const test = firstValueFrom(observable)

      await expect(test).resolves.toEqual(expectedUrl)
      expect(mockRouter.parseUrl).toHaveBeenCalledWith(expectedUrl)
    })
  })
})

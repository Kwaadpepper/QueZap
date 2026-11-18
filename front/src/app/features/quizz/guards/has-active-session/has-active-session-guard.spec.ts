import { signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import {
  ActivatedRouteSnapshot, CanActivateChildFn, CanActivateFn, GuardResult, Router, RouterStateSnapshot,
} from '@angular/router'

import { firstValueFrom, Observable } from 'rxjs'

import { Session, SessionCode, SessionId } from '@quezap/domain/models'

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
      const observable = runGuardInContext(hasActiveSessionGuard())
      const test = firstValueFrom(observable)

      await expect(test).resolves.toEqual(true)
    })

    it('hasActiveSessionChildGuard should return TRUE', async () => {
      const observable = runGuardInContext(hasActiveSessionChildGuard())
      const test = firstValueFrom(observable)

      await expect(test).resolves.toEqual(true)
    })
  })

  describe('If session has ended or is not running', () => {
    const baseSession: Session = {
      id: '018e3a2b-8888-7def-8123-000000000001' as SessionId,
      code: '20082' as SessionCode,
      name: 'Quiz test',
      startedAt: new Date(),
      endedAt: null,
    }

    beforeEach(() => {
      mockActiveSessionStore.restorationComplete.set(true)
    })

    it('should allow active running session when withQuizzRunning is true', async () => {
      mockActiveSessionStore.session.mockReturnValue(baseSession)

      const result = await firstValueFrom(runGuardInContext(hasActiveSessionGuard({ withQuizzRunning: true })))
      expect(result).toBe(true)
    })

    it('should redirect to /quizz/expired when session.endedAt is non-null (even if startedAt is set)', async () => {
      mockActiveSessionStore.session.mockReturnValue({
        ...baseSession,
        endedAt: new Date(),
      })

      const res1 = await firstValueFrom(runGuardInContext(hasActiveSessionGuard()))
      const res2 = await firstValueFrom(runGuardInContext(hasActiveSessionGuard({ withQuizzRunning: true })))

      expect(res1).toBe('/quizz/expired')
      expect(res2).toBe('/quizz/expired')
    })

    it('should redirect to /quizz/expired when session.status is ENDED', async () => {
      mockActiveSessionStore.session.mockReturnValue({
        ...baseSession,
        status: 'ENDED',
      })

      const res = await firstValueFrom(runGuardInContext(hasActiveSessionGuard({ withQuizzRunning: true })))
      expect(res).toBe('/quizz/expired')
    })

    it('should allow lobby session when withQuizzRunning is omitted, and reject when true', async () => {
      mockActiveSessionStore.session.mockReturnValue({
        ...baseSession,
        startedAt: null,
        endedAt: null,
      })

      const lobbyRes = await firstValueFrom(runGuardInContext(hasActiveSessionGuard()))
      const runnerRes = await firstValueFrom(runGuardInContext(hasActiveSessionGuard({ withQuizzRunning: true })))

      expect(lobbyRes).toBe(true)
      expect(runnerRes).toBe('/quizz/expired')
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

      const observable = runGuardInContext(hasActiveSessionGuard())
      const test = firstValueFrom(observable)

      await expect(test).resolves.toEqual(expectedUrl)
      expect(mockRouter.parseUrl).toHaveBeenCalledWith(expectedUrl)
    })

    it('hasActiveSessionChildGuard should return the redirection URL', async () => {
      const expectedUrl = '/quizz/expired'
      mockRouter.parseUrl.mockReturnValue(expectedUrl)

      const observable = runGuardInContext(hasActiveSessionChildGuard())
      const test = firstValueFrom(observable)

      await expect(test).resolves.toEqual(expectedUrl)
      expect(mockRouter.parseUrl).toHaveBeenCalledWith(expectedUrl)
    })
  })
})

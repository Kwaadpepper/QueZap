import { signal, WritableSignal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router'

import { firstValueFrom, Observable, of, Subject } from 'rxjs'

import {
  Participant, ParticipantId, QuestionId, QuestionType, Score, Session, SessionCode, SessionId,
} from '@quezap/domain/models'

import { hasActiveSessionGuard } from '../guards'
import {
  ActiveSessionPersistence,
  CurrentQuestion,
  SESSION_API_SERVICE,
  SESSION_OBSERVER_SERVICE,
  SessionApiService,
  SessionEvent,
  SessionObserverService,
} from '../services'

import { ActiveSessionStore } from './active-session'

describe('ActiveSessionStore', () => {
  let mockObserver: SessionObserverService & { connect: jest.Mock, disconnect: jest.Mock }
  let mockApi: Partial<SessionApiService>
  let events$: Subject<{ kind: 'success', result: SessionEvent }>

  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    events$ = new Subject()

    mockObserver = {
      sessionEvents: jest.fn(() => events$.asObservable()),
      participants: signal([]),
      question: signal<CurrentQuestion>({ type: 'WaitingQuestion' }),
      reveal: signal(undefined),
      leaderboard: signal(undefined),
      connect: jest.fn(),
      disconnect: jest.fn(),
    }

    mockApi = {
      joinSessionWith: jest.fn((code: SessionCode) => of({
        kind: 'success' as const,
        result: {
          id: '018e3a2b-8888-7def-8123-000000000001' as SessionId,
          code,
          name: 'Quiz test',
          status: 'STARTED',
          startedAt: new Date(),
          endedAt: null,
        } satisfies Session,
      })),
      chooseNickname: jest.fn(() => of({ kind: 'success' as const, result: undefined })),
      leaveSession: jest.fn(() => of({ kind: 'success' as const, result: undefined })),
    }

    TestBed.configureTestingModule({
      providers: [
        ActiveSessionStore,
        ActiveSessionPersistence,
        { provide: SESSION_OBSERVER_SERVICE, useValue: mockObserver },
        { provide: SESSION_API_SERVICE, useValue: mockApi },
      ],
    })
  })

  afterEach(() => {
    jest.restoreAllMocks()
    localStorage.clear()
    sessionStorage.clear()
  })

  it('should instantiate without emitting @ngrx/signals member override warnings', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => void 0)

    const store = TestBed.inject(ActiveSessionStore)

    expect(store).toBeTruthy()
    expect(store.isSessionPaused()).toBe(false)
    expect(store.mySubmittedChoice()).toBeUndefined()
    expect(warnSpy).not.toHaveBeenCalledWith(
      expect.stringContaining('SignalStore members cannot be overridden'),
    )
  })

  it('should connect to observer via interface method without instanceof coupling', () => {
    const store = TestBed.inject(ActiveSessionStore)
    store.joinSession('20082' as SessionCode).subscribe()
    TestBed.flushEffects()

    expect(mockObserver.connect).toHaveBeenCalledWith('018e3a2b-8888-7def-8123-000000000001')
  })

  it('should restore session, nickname without rememberMe, and submittedChoice after tab reopen', () => {
    const persistence = TestBed.inject(ActiveSessionPersistence)
    persistence.patch({ code: '20082' as SessionCode, sessionId: '018e3a2b-8888-7def-8123-000000000001' })
    persistence.saveParticipationData('018e3a2b-8888-7def-8123-000000000001', 'ptk_abc', 'Léa')
    persistence.persistNickname(undefined)
    persistence.saveSubmittedChoice(
      'q-1',
      { answerIndex: 2, label: 'Option C' },
      '018e3a2b-8888-7def-8123-000000000001',
    )

    // Simulate closed tab (sessionStorage empty)
    sessionStorage.clear()

    const store = TestBed.inject(ActiveSessionStore)
    TestBed.flushEffects()

    expect(store.restorationComplete()).toBe(true)
    expect(store.session()?.code).toBe('20082')
    expect(store.nickname()?.value()).toBe('Léa')
    expect(persistence.getSubmittedChoice('q-1')).toEqual({ answerIndex: 2, label: 'Option C' })
  })

  it('should stay in lobby on WaitingQuestion and allow /quizz after SESSION_STARTED and QUESTION_START', async () => {
    (mockApi.joinSessionWith as jest.Mock).mockReturnValueOnce(of({
      kind: 'success' as const,
      result: {
        id: '018e3a2b-8888-7def-8123-000000000001' as SessionId,
        code: '20082' as SessionCode,
        name: 'Quiz test',
        status: 'LOBBY',
        startedAt: null,
        endedAt: null,
      } satisfies Session,
    }))

    const store = TestBed.inject(ActiveSessionStore)
    store.joinSession('20082' as SessionCode).subscribe()
    store.chooseNickname('Léa', false).subscribe()
    TestBed.flushEffects()

    expect(store.sessionHasStarted()).toBe(false)

    const lobbyGuardRes = await firstValueFrom(
      TestBed.runInInjectionContext(() =>
        hasActiveSessionGuard()(null as unknown as ActivatedRouteSnapshot, null as unknown as RouterStateSnapshot),
      ) as Observable<unknown>,
    )
    expect(lobbyGuardRes).toBe(true)

    events$.next({
      kind: 'success',
      result: {
        type: 'SessionStarted',
        session: { startedAt: new Date('2026-11-20T20:00:00Z') },
      },
    })
    ;(mockObserver.question as WritableSignal<CurrentQuestion>).set({
      type: QuestionType.Boolean,
      id: 'q-1' as QuestionId,
      value: 'Vrai ou faux ?',
      limit: { seconds: 20 },
      answers: [
        { index: 0, points: 1000, value: 'Vrai' },
        { index: 1, points: 1000, value: 'Faux' },
      ],
    })
    events$.next({
      kind: 'success',
      result: {
        type: 'SessionSwitchedQuestion',
        questionId: 'q-1' as QuestionId,
      },
    })
    TestBed.flushEffects()

    expect(store.sessionHasStarted()).toBe(true)
    expect(store.session()?.startedAt).not.toBeNull()
    expect(store.session()?.status).toBe('STARTED')

    const quizzGuardRes = await firstValueFrom(
      TestBed.runInInjectionContext(() =>
        hasActiveSessionGuard({ withQuizzRunning: true })(
          null as unknown as ActivatedRouteSnapshot,
          null as unknown as RouterStateSnapshot,
        ),
      ) as Observable<unknown>,
    )
    expect(quizzGuardRes).toBe(true)
  })

  it('should redirect kicked participant to /quizz/ended with clear exclusion message', () => {
    const router = TestBed.inject(Router)
    const navSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true)

    const store = TestBed.inject(ActiveSessionStore)
    store.joinSession('20082' as SessionCode).subscribe()
    store.chooseNickname('Léa', false).subscribe()
    TestBed.flushEffects()

    events$.next({
      kind: 'success',
      result: {
        type: 'SessionKicked',
        participantName: 'Léa',
      },
    })

    expect(mockObserver.disconnect).toHaveBeenCalled()
    expect(navSpy).toHaveBeenCalledWith(['/quizz/ended'], {
      state: {
        kicked: true,
        message: 'Vous avez été exclu de la session',
      },
    })
  })

  it('should deduplicate case-insensitively and keep canonical nickname returned by API', () => {
    ;(mockObserver.participants as WritableSignal<Participant[]>).set([
      {
        id: '018e3a2b-0000-7000-8000-000000000001' as ParticipantId,
        nickname: 'Salut',
        score: 0 as Score,
      },
    ])
    ;(mockApi.chooseNickname as jest.Mock).mockReturnValueOnce(of({ kind: 'success', result: 'Salut' }))

    const store = TestBed.inject(ActiveSessionStore)
    TestBed.flushEffects()

    store.chooseNickname('SAlut', true).subscribe()
    TestBed.flushEffects()

    expect(store.nickname()?.value()).toBe('Salut')
    expect(store.participants().map(p => p.nickname)).toEqual(['Salut'])
  })
})

import { signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'

import { of, Subject } from 'rxjs'

import { Session, SessionCode, SessionId } from '@quezap/domain/models'

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
})

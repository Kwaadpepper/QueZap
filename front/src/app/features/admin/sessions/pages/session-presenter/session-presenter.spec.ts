import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'

import { MessageService } from 'primeng/api'
import { of } from 'rxjs'

import { NotFoundError, UnauthorizedError } from '@quezap/core/errors'
import { isAuthenticatedGuard } from '@quezap/core/guards/auth/authenticated-guard'

import { routes as featureRoutes } from '../../../../routes'
import { SESSION_SERVICE } from '../../services/session'

import { SessionPresenter } from './session-presenter'

describe('SessionPresenter', () => {
  let component: SessionPresenter
  let fixture: ComponentFixture<SessionPresenter>
  let router: Router

  const mockMessageService = { add: jest.fn() }

  const mockSessionService = {
    getSession: jest.fn().mockReturnValue(
      of({
        kind: 'success',
        result: { id: 'test-session-id', code: 'ABCDEF', name: 'Test Session', status: 'WAITING' },
      }),
    ),
    getScores: jest.fn().mockReturnValue(of({ kind: 'success', result: { leaderboard: [] } })),
    startSession: jest.fn().mockReturnValue(of({ kind: 'success', result: undefined })),
    nextQuestion: jest.fn().mockReturnValue(of({ kind: 'success', result: undefined })),
    revealAnswers: jest.fn().mockReturnValue(of({ kind: 'success', result: undefined })),
    exportCsv: jest.fn().mockReturnValue(of({ kind: 'success', result: new Blob(['name,score\nAlice,100']) })),
    kickParticipant: jest.fn().mockReturnValue(of({ kind: 'success', result: undefined })),
    pauseSession: jest.fn().mockImplementation((_id: string, paused: boolean) =>
      of({ kind: 'success', result: { isPaused: paused } }),
    ),
    updateSettings: jest.fn().mockReturnValue(of({ kind: 'success', result: {} })),
  }

  beforeEach(async () => {
    jest.clearAllMocks()
    mockSessionService.getSession.mockReturnValue(
      of({
        kind: 'success',
        result: { id: 'test-session-id', code: 'ABCDEF', name: 'Test Session', status: 'WAITING' },
      }),
    )
    mockSessionService.startSession.mockReturnValue(of({ kind: 'success', result: undefined }))
    mockSessionService.nextQuestion.mockReturnValue(of({ kind: 'success', result: undefined }))
    mockSessionService.revealAnswers.mockReturnValue(of({ kind: 'success', result: undefined }))
    mockSessionService.exportCsv.mockReturnValue(of({ kind: 'success', result: new Blob(['name,score\nAlice,100']) }))
    mockSessionService.kickParticipant.mockReturnValue(of({ kind: 'success', result: undefined }))
    mockSessionService.pauseSession.mockImplementation((_id: string, paused: boolean) =>
      of({ kind: 'success', result: { isPaused: paused } }),
    )
    mockSessionService.updateSettings.mockReturnValue(of({ kind: 'success', result: {} }))

    await TestBed.configureTestingModule({
      imports: [SessionPresenter],
      providers: [
        provideRouter([]),
        { provide: SESSION_SERVICE, useValue: mockSessionService },
        { provide: MessageService, useValue: mockMessageService },
      ],
    }).compileComponents()

    router = TestBed.inject(Router)
    fixture = TestBed.createComponent(SessionPresenter)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  afterEach(() => {
    jest.useRealTimers()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should transition from LOBBY to QUESTION when startQuiz is called', () => {
    expect(component['stage']()).toBe('LOBBY')
    component['startQuiz']()
    expect(component['stage']()).toBe('QUESTION')
    expect(mockSessionService.startSession).toHaveBeenCalled()
  })

  it('should transition to REVEAL and call revealAnswers when revealAnswers is called', () => {
    component['revealAnswers']()
    expect(component['stage']()).toBe('REVEAL')
    expect(mockSessionService.revealAnswers).toHaveBeenCalled()
  })

  it('should progress through podium steps based on leaderboard size and allow skipping', () => {
    jest.useFakeTimers()
    // 0 players -> immediate reveal without waiting
    component['leaderboard'].set([])
    component['launchPodiumAnimation']()
    expect(component['podiumStep']()).toBe(3)

    // 1 player -> skips empty 3rd/2nd steps
    component['leaderboard'].set([{ name: 'Salut', score: 1000 }])
    component['launchPodiumAnimation']()
    expect(component['podiumStep']()).toBe(2)
    jest.advanceTimersByTime(650)
    expect(component['podiumStep']()).toBe(3)
    jest.advanceTimersByTime(100)

    // 3 players -> reveals 3rd, 2nd, 1st cleanly
    component['leaderboard'].set([
      { name: 'Alice', score: 3000 },
      { name: 'Bob', score: 2000 },
      { name: 'Charlie', score: 1000 },
    ])
    component['launchPodiumAnimation']()
    expect(component['podiumStep']()).toBe(0)

    jest.advanceTimersByTime(450)
    expect(component['podiumStep']()).toBe(1)

    jest.advanceTimersByTime(700)
    expect(component['podiumStep']()).toBe(2)

    jest.advanceTimersByTime(800)
    expect(component['podiumStep']()).toBe(3)
    jest.advanceTimersByTime(100)

    component['launchPodiumAnimation']()
    expect(component['podiumStep']()).toBe(0)
    component['skipPodiumAnimation']()
    expect(component['podiumStep']()).toBe(3)
    jest.advanceTimersByTime(100)
  })

  it('should toggle qrZoomed when toggleQrZoom is called', () => {
    expect(component['qrZoomed']()).toBe(false)
    component['toggleQrZoom']()
    expect(component['qrZoomed']()).toBe(true)
    component['toggleQrZoom']()
    expect(component['qrZoomed']()).toBe(false)
  })

  it('should compute displayJoinUrl dynamically based on host', () => {
    expect(component['displayJoinUrl']()).toContain('/quizz/join')
  })

  it('should format BOOLEAN question with Vrai and Faux and correct choice colors', () => {
    component['handleEvent']({
      type: 'QUESTION_START',
      payload: {
        slideIndex: 1,
        totalSlides: 1,
        timerSeconds: 30,
        question: {
          id: 'q1',
          type: 'BOOLEAN',
          label: 'La Terre est ronde ?',
          answers: [{ id: 'ans1', label: 'True' }],
        },
      },
    })

    const q = component['currentQuestion']()
    expect(q.answers.length).toBe(2)
    expect(q.answers[0].label).toBe('Vrai')
    expect(q.answers[1].label).toBe('Faux')
    expect(component['getChoiceColorClass'](0)).toBe('choice-green')
    expect(component['getChoiceColorClass'](1)).toBe('choice-red')
  })

  it('should return appropriate shape icons for choices', () => {
    expect(component['getChoiceForm'](0)).toBe('circle')
    expect(component['getChoiceForm'](1)).toBe('diamond')
    expect(component['getChoiceForm'](2)).toBe('cross')
    expect(component['getChoiceForm'](3)).toBe('hexagon')
  })

  it('should restore answeredCount from QUESTION_START payload', () => {
    component['handleEvent']({
      type: 'QUESTION_START',
      payload: {
        slideIndex: 1,
        totalSlides: 2,
        timerSeconds: 20,
        answeredCount: 4,
        question: {
          id: 'q1',
          type: 'QUIZZ',
          label: 'Test',
          answers: [
            { id: 'a1', label: '1' },
            { id: 'a2', label: '2' },
          ],
        },
      },
    })

    expect(component['answersReceived']()).toBe(4)
  })

  it('should update intermediateLeaderboard and transition to LEADERBOARD on showIntermediateLeaderboard', () => {
    component['handleEvent']({
      type: 'INTERMEDIATE_LEADERBOARD',
      payload: {
        slideIndex: 1,
        totalSlides: 3,
        leaderboard: [
          { rank: 1, participantName: 'Alice', score: 1000, streak: 3 },
          { rank: 2, participantName: 'Bob', score: 800, streak: 1 },
        ],
      },
    })

    expect(component['intermediateLeaderboard']().length).toBe(2)
    expect(component['intermediateLeaderboard']()[0].participantName).toBe('Alice')

    component['showIntermediateLeaderboard']()
    expect(component['stage']()).toBe('LEADERBOARD')
  })

  it('should automatically reveal answers at countdown 0 when autoReveal is enabled', () => {
    jest.useFakeTimers()
    component.autoReveal.set(true)
    mockSessionService.revealAnswers.mockClear()

    component['handleEvent']({
      type: 'QUESTION_START',
      payload: {
        slideIndex: 1,
        totalSlides: 2,
        timerSeconds: 3,
        question: {
          id: 'q1',
          type: 'QUIZZ',
          label: 'Test',
          answers: [
            { id: 'a1', label: '1' },
            { id: 'a2', label: '2' },
          ],
        },
      },
    })

    expect(component['timer']()).toBe(3)
    jest.advanceTimersByTime(3500)
    expect(component['timer']()).toBe(0)
    expect(mockSessionService.revealAnswers).toHaveBeenCalled()
    expect(component['stage']()).toBe('REVEAL')
  })

  it('should not automatically reveal answers at countdown 0 when autoReveal is disabled', () => {
    jest.useFakeTimers()
    component.autoReveal.set(false)
    mockSessionService.revealAnswers.mockClear()

    component['handleEvent']({
      type: 'QUESTION_START',
      payload: {
        slideIndex: 1,
        totalSlides: 2,
        timerSeconds: 3,
        question: {
          id: 'q1',
          type: 'QUIZZ',
          label: 'Test',
          answers: [
            { id: 'a1', label: '1' },
            { id: 'a2', label: '2' },
          ],
        },
      },
    })

    jest.advanceTimersByTime(3500)
    expect(component['timer']()).toBe(0)
    expect(mockSessionService.revealAnswers).not.toHaveBeenCalled()
  })

  it('should manage accessible kick confirmation workflow safely', () => {
    mockSessionService.kickParticipant.mockClear()
    expect(component['playerToKick']()).toBeNull()

    component['requestKick']('BadPlayer')
    expect(component['playerToKick']()).toBe('BadPlayer')
    expect(mockSessionService.kickParticipant).not.toHaveBeenCalled()

    component['cancelKick']()
    expect(component['playerToKick']()).toBeNull()
    expect(mockSessionService.kickParticipant).not.toHaveBeenCalled()

    component['requestKick']('BadPlayer')
    component['confirmKick']()
    expect(mockSessionService.kickParticipant).toHaveBeenCalledWith(expect.any(String), 'BadPlayer')
    expect(component['playerToKick']()).toBeNull()
  })

  it('should allow returning to REVEAL view from LEADERBOARD view', () => {
    component['showIntermediateLeaderboard']()
    expect(component['stage']()).toBe('LEADERBOARD')

    component['stage'].set('REVEAL')
    expect(component['stage']()).toBe('REVEAL')
  })

  it('should toggle progression mode and call updateSettings', () => {
    expect(component['progressionMode']()).toBe('MANUAL')
    component['toggleProgressionMode']()
    expect(component['progressionMode']()).toBe('AUTOMATIC')
    expect(mockSessionService.updateSettings).toHaveBeenCalledWith('test-session-id', {
      progressionMode: 'AUTOMATIC',
      explanationDurationSeconds: 15,
      mobileCorrectionEnabled: false,
    })

    component['toggleProgressionMode']()
    expect(component['progressionMode']()).toBe('MANUAL')
  })

  it('should toggle pause, call pauseSession API, and freeze timer countdown', () => {
    jest.useFakeTimers()
    component['handleEvent']({
      type: 'QUESTION_START',
      payload: {
        slideIndex: 1,
        totalSlides: 1,
        timerSeconds: 10,
      },
    })
    expect(component['timer']()).toBe(10)
    expect(component['isPaused']()).toBe(false)

    jest.advanceTimersByTime(2000)
    expect(component['timer']()).toBe(8)

    // Pause
    component['togglePause']()
    expect(component['isPaused']()).toBe(true)
    expect(mockSessionService.pauseSession).toHaveBeenCalledWith('test-session-id', true)

    // Timer should be frozen while paused
    jest.advanceTimersByTime(3000)
    expect(component['timer']()).toBe(8)

    // Resume
    component['togglePause']()
    expect(component['isPaused']()).toBe(false)
    expect(mockSessionService.pauseSession).toHaveBeenCalledWith('test-session-id', false)

    jest.advanceTimersByTime(2000)
    expect(component['timer']()).toBe(6)
  })

  it('should handle SESSION_PAUSE_STATUS SSE event', () => {
    expect(component['isPaused']()).toBe(false)
    component['handleEvent']({
      type: 'SESSION_PAUSE_STATUS',
      payload: { isPaused: true } as unknown as SsePayload,
    })
    expect(component['isPaused']()).toBe(true)

    component['handleEvent']({
      type: 'SESSION_PAUSE_STATUS',
      payload: { isPaused: false } as unknown as SsePayload,
    })
    expect(component['isPaused']()).toBe(false)
  })

  it('should auto-advance after explanation countdown in AUTOMATIC mode during REVEAL', () => {
    jest.useFakeTimers()
    mockSessionService.nextQuestion.mockClear()
    component['setProgressionMode']('AUTOMATIC')
    component['setExplanationDuration'](5)

    component['handleEvent']({
      type: 'QUESTION_REVEAL',
      payload: {
        slideIndex: 1,
        correctAnswerIndices: [0],
      },
    })

    expect(component['stage']()).toBe('REVEAL')
    expect(component['explanationTimer']()).toBe(5)

    jest.advanceTimersByTime(3000)
    expect(component['explanationTimer']()).toBe(2)
    expect(mockSessionService.nextQuestion).not.toHaveBeenCalled()

    jest.advanceTimersByTime(2500)
    expect(component['explanationTimer']()).toBe(0)
    expect(mockSessionService.nextQuestion).toHaveBeenCalled()
  })

  it('should auto-advance after countdown in AUTOMATIC mode when viewing LEADERBOARD', () => {
    jest.useFakeTimers()
    mockSessionService.nextQuestion.mockClear()
    component['setProgressionMode']('AUTOMATIC')
    component['setExplanationDuration'](5)

    component['handleEvent']({
      type: 'QUESTION_REVEAL',
      payload: {
        slideIndex: 1,
        correctAnswerIndices: [0],
      },
    })

    expect(component['stage']()).toBe('REVEAL')
    expect(component['explanationTimer']()).toBe(5)

    jest.advanceTimersByTime(2000)
    expect(component['explanationTimer']()).toBe(3)

    // Presenter clicks intermediate leaderboard during countdown
    component['showIntermediateLeaderboard']()
    expect(component['stage']()).toBe('LEADERBOARD')

    jest.advanceTimersByTime(3500)
    expect(component['explanationTimer']()).toBe(0)
    expect(mockSessionService.nextQuestion).toHaveBeenCalled()
  })

  it('should store class metrics on SESSION_ENDED event', () => {
    component['handleEvent']({
      type: 'SESSION_ENDED',
      payload: {
        metrics: {
          totalParticipants: 20,
          averageScore: 7500,
          globalSuccessRate: 85.0,
        },
      } as unknown as Parameters<SessionPresenter['handleEvent']>[0]['payload'],
    })

    expect(component['classMetrics']()).toEqual({
      totalParticipants: 20,
      averageScore: 7500,
      globalSuccessRate: 85.0,
    })
  })

  it('should protect /projector/:id and /present/:id routes with isAuthenticatedGuard', () => {
    const projectorRoute = featureRoutes.find(r => r.path === 'projector/:id')
    const presentRoute = featureRoutes.find(r => r.path === 'present/:id')
    expect(projectorRoute?.canActivate).toContain(isAuthenticatedGuard)
    expect(presentRoute?.canActivate).toContain(isAuthenticatedGuard)
  })

  it('should toggle mobileCorrectionEnabled and send full settings payload', () => {
    expect(component['mobileCorrectionEnabled']()).toBe(false)
    component['toggleMobileCorrection']()
    expect(component['mobileCorrectionEnabled']()).toBe(true)
    expect(mockSessionService.updateSettings).toHaveBeenCalledWith('test-session-id', {
      progressionMode: 'MANUAL',
      explanationDurationSeconds: 15,
      mobileCorrectionEnabled: true,
    })
  })

  it('should rollback and display error toast on presenter command failures', () => {
    mockSessionService.startSession.mockReturnValueOnce(
      of({ kind: 'failure', error: new NotFoundError('Session introuvable') }),
    )
    component['startQuiz']()
    expect(component['stage']()).toBe('LOBBY')
    expect(mockMessageService.add).toHaveBeenCalledWith({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Session introuvable',
    })

    mockSessionService.pauseSession.mockReturnValueOnce(
      of({ kind: 'failure', error: new NotFoundError('Pause refusée') }),
    )
    component['togglePause']()
    expect(component['isPaused']()).toBe(false)
    expect(mockMessageService.add).toHaveBeenCalledWith({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Pause refusée',
    })

    mockSessionService.updateSettings.mockReturnValueOnce(
      of({ kind: 'failure', error: new NotFoundError('Réglages refusés') }),
    )
    component['toggleMobileCorrection']()
    expect(component['mobileCorrectionEnabled']()).toBe(false)
    expect(mockMessageService.add).toHaveBeenCalledWith({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Réglages refusés',
    })
  })

  it('should redirect to /auth/login when a presenter command returns UnauthorizedError (401)', () => {
    const navigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true)
    mockSessionService.nextQuestion.mockReturnValueOnce(
      of({ kind: 'failure', error: new UnauthorizedError('Session expirée') }),
    )

    component['nextStep']()

    expect(mockMessageService.add).toHaveBeenCalledWith({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Session expirée',
    })
    expect(navigateSpy).toHaveBeenCalledWith(['/auth/login'])
  })

  it('should export CSV via SessionService.exportCsv and toast on failure', () => {
    const createObjectUrlSpy = jest.fn().mockReturnValue('blob:mock-url')
    const revokeObjectUrlSpy = jest.fn()
    Object.defineProperty(window.URL, 'createObjectURL', { value: createObjectUrlSpy, writable: true })
    Object.defineProperty(window.URL, 'revokeObjectURL', { value: revokeObjectUrlSpy, writable: true })

    component['downloadCsv']()
    expect(mockSessionService.exportCsv).toHaveBeenCalledWith('test-session-id')
    expect(createObjectUrlSpy).toHaveBeenCalled()
    expect(revokeObjectUrlSpy).toHaveBeenCalledWith('blob:mock-url')

    mockSessionService.exportCsv.mockReturnValueOnce(
      of({ kind: 'failure', error: new NotFoundError('CSV indisponible') }),
    )
    component['downloadCsv']()
    expect(mockMessageService.add).toHaveBeenCalledWith({
      severity: 'error',
      summary: 'Erreur',
      detail: 'CSV indisponible',
    })
  })

  it('should synchronize timer with payload.deadline in QUESTION_START', () => {
    jest.useFakeTimers()
    const futureDeadline = new Date(Date.now() + 12000).toISOString()
    component['handleEvent']({
      type: 'QUESTION_START',
      payload: {
        slideIndex: 1,
        totalSlides: 2,
        timerSeconds: 20,
        deadline: futureDeadline,
        question: {
          id: 'q1',
          type: 'QUIZZ',
          label: 'Deadline Test',
          answers: [{ id: 'a1', label: 'A' }, { id: 'a2', label: 'B' }],
        },
      },
    })

    expect(component['timer']()).toBe(12)
    jest.advanceTimersByTime(3000)
    expect(component['timer']()).toBe(9)
  })

  it('should enter SYNCING stage on reload when session is STARTED until QUESTION_START arrives', () => {
    mockSessionService.getSession.mockReturnValueOnce(
      of({
        kind: 'success',
        result: {
          id: 'started-id',
          code: 'XYZ123',
          name: 'Started Session',
          status: 'STARTED',
          startedAt: '2026-11-20T18:00:00Z',
          endedAt: undefined,
        },
      }),
    )

    const reloadFixture = TestBed.createComponent(SessionPresenter)
    const reloadComponent = reloadFixture.componentInstance
    reloadFixture.detectChanges()

    expect(reloadComponent['stage']()).toBe('SYNCING')

    reloadComponent['handleEvent']({
      type: 'QUESTION_START',
      payload: {
        slideIndex: 2,
        totalSlides: 5,
        timerSeconds: 20,
        question: { id: 'q2', type: 'QUIZZ', label: 'Question 2', answers: [] },
      },
    })

    expect(reloadComponent['stage']()).toBe('QUESTION')
    expect(reloadComponent['currentQuestionIndex']()).toBe(1)
    expect(reloadComponent['totalQuestions']()).toBe(5)
  })
})

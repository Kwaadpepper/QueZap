import { provideHttpClient } from '@angular/common/http'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing'
import { provideRouter } from '@angular/router'

import { of } from 'rxjs'

import { SESSION_SERVICE } from '../../services/session'

import { SessionPresenter } from './session-presenter'

describe('SessionPresenter', () => {
  let component: SessionPresenter
  let fixture: ComponentFixture<SessionPresenter>

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
    kickParticipant: jest.fn().mockReturnValue(of({ kind: 'success', result: undefined })),
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SessionPresenter],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SESSION_SERVICE, useValue: mockSessionService },
      ],
    }).compileComponents()

    fixture = TestBed.createComponent(SessionPresenter)
    component = fixture.componentInstance
    fixture.detectChanges()
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

  it('should progress through podium steps during launchPodiumAnimation and allow skipping', fakeAsync(() => {
    component['launchPodiumAnimation']()
    expect(component['podiumStep']()).toBe(0)

    tick(1200)
    expect(component['podiumStep']()).toBe(1)

    tick(1400)
    expect(component['podiumStep']()).toBe(2)

    tick(1400)
    expect(component['podiumStep']()).toBe(3)

    component['launchPodiumAnimation']()
    expect(component['podiumStep']()).toBe(0)
    component['skipPodiumAnimation']()
    expect(component['podiumStep']()).toBe(3)
  }))

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

  it('should automatically reveal answers at countdown 0 when autoReveal is enabled', fakeAsync(() => {
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
    tick(3500)
    expect(component['timer']()).toBe(0)
    expect(mockSessionService.revealAnswers).toHaveBeenCalled()
    expect(component['stage']()).toBe('REVEAL')
  }))

  it('should not automatically reveal answers at countdown 0 when autoReveal is disabled', fakeAsync(() => {
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

    tick(3500)
    expect(component['timer']()).toBe(0)
    expect(mockSessionService.revealAnswers).not.toHaveBeenCalled()
  }))

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
})

import { provideHttpClient } from '@angular/common/http'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { ActivatedRoute, provideRouter } from '@angular/router'

import { of } from 'rxjs'

import { SessionReportDto } from '@quezap/core/api-client'
import { NotFoundError } from '@quezap/core/errors'

import { SESSION_SERVICE } from '../../services/session'

import { SessionReport } from './session-report'

describe('SessionReport', () => {
  let component: SessionReport
  let fixture: ComponentFixture<SessionReport>

  const sessionId = '018e3a2b-8888-7def-8123-abcdef888888'

  const mockReport: SessionReportDto = {
    sessionId,
    sessionNumber: 1042,
    sessionCode: 'QZ-1042',
    sessionName: 'Session Finale',
    startedAt: '2026-10-11T10:00:00Z',
    endedAt: '2026-10-11T10:30:00Z',
    totalParticipants: 2,
    totalQuestions: 1,
    averageScore: 500,
    averageAccuracyRate: 50,
    participants: [
      {
        rank: 1,
        participantName: 'Alice',
        totalScore: 1000,
        correctCount: 1,
        totalQuestions: 1,
        accuracyRate: 100,
      },
      {
        rank: 2,
        participantName: 'Bob',
        totalScore: 0,
        correctCount: 0,
        totalQuestions: 1,
        accuracyRate: 0,
      },
    ],
    questions: [
      {
        questionOrder: 1,
        questionLabel: 'Speed of light?',
        questionType: 'QUIZZ',
        totalAnswers: 2,
        correctAnswers: 1,
        successRate: 50,
        answers: [
          {
            answerIndex: 0,
            label: '300,000 km/s',
            isCorrect: true,
            count: 1,
            percentage: 50,
          },
          {
            answerIndex: 1,
            label: '150,000 km/s',
            isCorrect: false,
            count: 1,
            percentage: 50,
          },
        ],
      },
    ],
  }

  const successResult = of({ kind: 'success' as const, result: mockReport })
  const mockSessionService = { getSessionReport: jest.fn().mockReturnValue(successResult) }

  const mockRoute = { snapshot: { paramMap: { get: jest.fn().mockReturnValue(sessionId) } } }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SessionReport],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: mockRoute },
        { provide: SESSION_SERVICE, useValue: mockSessionService },
      ],
    }).compileComponents()

    fixture = TestBed.createComponent(SessionReport)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create and load report on init', () => {
    expect(component).toBeTruthy()
    expect(mockSessionService.getSessionReport).toHaveBeenCalledWith(sessionId)
    expect(component['report']()).toEqual(mockReport)
    expect(component['loading']()).toBe(false)
  })

  it('should display error when report fetching fails', () => {
    mockSessionService.getSessionReport.mockReturnValueOnce(
      of({ kind: 'failure', error: new NotFoundError('Session introuvable') }),
    )
    component['fetchReport'](sessionId)
    expect(component['error']()).toBe('Session introuvable')
    expect(component['loading']()).toBe(false)
  })

  it('should determine success badge severity based on rate', () => {
    expect(component['getSuccessBadgeSeverity'](85)).toBe('success')
    expect(component['getSuccessBadgeSeverity'](55)).toBe('warn')
    expect(component['getSuccessBadgeSeverity'](25)).toBe('danger')
  })
})

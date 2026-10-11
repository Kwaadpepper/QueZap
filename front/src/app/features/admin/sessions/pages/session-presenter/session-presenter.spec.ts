import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'

import { of } from 'rxjs'

import { SESSION_SERVICE } from '../../services/session'

import { SessionPresenter } from './session-presenter'

describe('SessionPresenter', () => {
  let component: SessionPresenter
  let fixture: ComponentFixture<SessionPresenter>

  const mockSessionService = {
    getSession: jest.fn().mockReturnValue(
      of({ kind: 'success', result: { code: 'ABCDEF', name: 'Test Session', status: 'WAITING' } }),
    ),
    getScores: jest.fn().mockReturnValue(of({ kind: 'success', result: { leaderboard: [] } })),
    startSession: jest.fn().mockReturnValue(of({ kind: 'success', result: undefined })),
    nextQuestion: jest.fn().mockReturnValue(of({ kind: 'success', result: undefined })),
    kickParticipant: jest.fn().mockReturnValue(of({ kind: 'success', result: undefined })),
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SessionPresenter],
      providers: [
        provideRouter([]),
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

  it('should transition to REVEAL when revealAnswers is called', () => {
    component['revealAnswers']()
    expect(component['stage']()).toBe('REVEAL')
  })
})

import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'

import { of } from 'rxjs'

import { SessionHttpService } from '../../services/session-http'

import { SessionPresenter } from './session-presenter'

describe('SessionPresenter', () => {
  let component: SessionPresenter
  let fixture: ComponentFixture<SessionPresenter>

  const mockSessionHttpService = {
    startSession: jest.fn().mockReturnValue(of(void 0)),
    nextQuestion: jest.fn().mockReturnValue(of(void 0)),
    kickParticipant: jest.fn().mockReturnValue(of(void 0)),
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SessionPresenter],
      providers: [
        provideRouter([]),
        { provide: SessionHttpService, useValue: mockSessionHttpService },
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
    expect(mockSessionHttpService.startSession).toHaveBeenCalled()
  })

  it('should transition to REVEAL when revealAnswers is called', () => {
    component['revealAnswers']()
    expect(component['stage']()).toBe('REVEAL')
  })
})

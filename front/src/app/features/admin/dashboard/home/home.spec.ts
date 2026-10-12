import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'

import { of } from 'rxjs'

import { QuezapWithTheme } from '@quezap/domain/models'

import { QUEZAP_SERVICE } from '../../quezaps/services/quezap'
import { SESSION_SERVICE } from '../../sessions/services/session'

import { Home } from './home'

describe('Home', () => {
  let component: Home
  let fixture: ComponentFixture<Home>

  const mockQuezapService = {
    getQuezapPage: jest.fn().mockReturnValue(
      of({
        kind: 'success',
        result: {
          data: [
            {
              id: 'quiz-1',
              title: 'Quiz Maths',
              theme: { id: 'theme-1', name: 'Maths' },
            } as QuezapWithTheme,
          ],
          totalElements: 1,
          totalPages: 1,
          page: 1,
          pageSize: 4,
          hasNext: false,
          hasPrevious: false,
        },
      }),
    ),
  }

  const mockSessionService = {
    getSessionPage: jest.fn().mockReturnValue(
      of({
        kind: 'success',
        result: {
          data: [],
          totalElements: 3,
          totalPages: 1,
          page: 1,
          pageSize: 4,
          hasNext: false,
          hasPrevious: false,
        },
      }),
    ),
    createSession: jest.fn().mockReturnValue(
      of({
        kind: 'success',
        result: {
          id: 'session-xyz',
          sessionNumber: 12345,
          name: 'Session',
        },
      }),
    ),
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [
        provideRouter([]),
        { provide: QUEZAP_SERVICE, useValue: mockQuezapService },
        { provide: SESSION_SERVICE, useValue: mockSessionService },
      ],
    }).compileComponents()

    fixture = TestBed.createComponent(Home)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should render pedagogical metrics instead of technical jargon', () => {
    const compiled = fixture.nativeElement as HTMLElement
    expect(compiled.textContent).toContain('Participants Accueillis')
    expect(compiled.textContent).toContain('Taux de Réussite Moyen')
    expect(compiled.textContent).not.toContain('SSE Direct')
    expect(compiled.textContent).not.toContain('16:9 HD')
  })

  it('should launch session directly when launch button is clicked', () => {
    const openSpy = jest.spyOn(window, 'open').mockImplementation(() => null)
    const quiz = {
      id: 'quiz-1',
      title: 'Quiz Maths',
      theme: { id: 'theme-1', name: 'Maths' },
    } as QuezapWithTheme

    component['launchQuiz'](quiz)

    expect(mockSessionService.createSession).toHaveBeenCalledWith(
      'quiz-1',
      expect.stringContaining('Quiz Maths -'),
    )
    expect(openSpy).toHaveBeenCalledWith(
      expect.stringContaining('/projector/session-xyz'),
      '_blank',
    )
    openSpy.mockRestore()
  })
})

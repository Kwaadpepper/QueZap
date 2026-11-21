import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'

import { MessageService } from '@openng/optimus-ui/api'

import { of, Subject } from 'rxjs'

import { NotFoundError } from '@quezap/core/errors'
import { QuezapWithTheme } from '@quezap/domain/models'

import { QUEZAP_SERVICE } from '../../../quezaps/services/quezap'
import { SESSION_SERVICE } from '../../services/session'

import { SessionCreate } from './session-create'

describe('SessionCreate', () => {
  let component: SessionCreate
  let fixture: ComponentFixture<SessionCreate>
  let router: Router

  const mockQuezapService = {
    getQuezapPage: jest.fn().mockReturnValue(
      of({
        kind: 'success' as const,
        result: {
          data: [
            { id: 'quiz-1', title: 'Maths', totalQuestions: 5 } as QuezapWithTheme,
          ],
          totalElements: 1,
          totalPages: 1,
          page: 1,
          pageSize: 50,
          hasNext: false,
          hasPrevious: false,
        },
      }),
    ),
  }

  const mockSessionService = { createSession: jest.fn() }

  const mockMessageService = { add: jest.fn() }

  beforeEach(async () => {
    jest.clearAllMocks()
    await TestBed.configureTestingModule({
      imports: [SessionCreate],
      providers: [
        provideRouter([]),
        { provide: QUEZAP_SERVICE, useValue: mockQuezapService },
        { provide: SESSION_SERVICE, useValue: mockSessionService },
        { provide: MessageService, useValue: mockMessageService },
      ],
    }).compileComponents()

    router = TestBed.inject(Router)
    fixture = TestBed.createComponent(SessionCreate)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create and load quezaps', () => {
    expect(component).toBeTruthy()
    expect(component.quezapOptions().length).toBe(1)
  })

  it('should track submitting() and navigate to presenter when presentAfterCreate is true', async () => {
    const response$ = new Subject<{ kind: 'success', result: { id: string, code: string } }>()
    mockSessionService.createSession.mockReturnValueOnce(response$.asObservable())
    const navigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true)

    component.model.set({ name: 'Séance 1', quezapId: 'quiz-1' })
    fixture.detectChanges()

    const savePromise = component.onSave(true)
    expect(component.sessionForm().submitting()).toBe(true)

    response$.next({ kind: 'success', result: { id: 'sess-42', code: 'QZ4242' } })
    response$.complete()
    await savePromise

    expect(component.sessionForm().submitting()).toBe(false)
    expect(mockSessionService.createSession).toHaveBeenCalledWith('quiz-1', 'Séance 1')
    expect(navigateSpy).toHaveBeenCalledWith(['/admin/sessions', 'sess-42', 'present'])
  })

  it('should show error toast when creation fails', async () => {
    mockSessionService.createSession.mockReturnValueOnce(
      of({ kind: 'failure', error: new NotFoundError('Quiz introuvable') }),
    )

    component.model.set({ name: 'Séance 2', quezapId: 'quiz-1' })
    fixture.detectChanges()

    await component.onSave(false)

    expect(mockMessageService.add).toHaveBeenCalledWith({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Quiz introuvable',
    })
  })
})

import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'

import { MessageService } from 'primeng/api'
import { of } from 'rxjs'

import { QuezapId } from '@quezap/domain/models'

import { SESSION_SERVICE } from '../../../sessions/services/session'
import { QuezapEditorStore } from '../../stores'

import { QuezapEditor } from './quezap-editor'

describe('QuezapEditor', () => {
  let component: QuezapEditor
  let fixture: ComponentFixture<QuezapEditor>

  const mockSessionService = {
    createSession: jest.fn().mockReturnValue(
      of({ kind: 'success', result: { id: 's1', code: 'ABC' } }),
    ),
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuezapEditor],
      providers: [
        provideRouter([]),
        MessageService,
        { provide: SESSION_SERVICE, useValue: mockSessionService },
      ],
    }).compileComponents()

    fixture = TestBed.createComponent(QuezapEditor)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should expose savedQuezapId when store quezap gets an id', () => {
    expect(component['savedQuezapId']()).toBeUndefined()
    const store = fixture.debugElement.injector.get(QuezapEditorStore)
    store.setQuezap({
      id: '018e3a2b-1111-7def-8123-abcdef111111' as QuezapId,
      title: 'Test',
      description: '',
      questionWithAnswersAndResponses: store.questions(),
    })
    expect(component['savedQuezapId']()).toBe('018e3a2b-1111-7def-8123-abcdef111111')
  })
})

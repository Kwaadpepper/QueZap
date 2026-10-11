import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'

import { MessageService } from 'primeng/api'
import { of } from 'rxjs'

import { SESSION_SERVICE } from '../../../sessions/services/session'

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
})

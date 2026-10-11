import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'

import { MessageService } from 'primeng/api'
import { of } from 'rxjs'

import { SESSION_SERVICE } from '../../../sessions/services/session'
import { QUEZAP_SERVICE } from '../../services/quezap'

import { QuezapCard } from './quezap-card'

describe('QuezapCard', () => {
  let component: QuezapCard
  let fixture: ComponentFixture<QuezapCard>

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuezapCard],
      providers: [
        provideRouter([]),
        MessageService,
        {
          provide: SESSION_SERVICE,
          useValue: {
            createSession: jest.fn().mockReturnValue(
              of({ kind: 'success', result: { id: 's1', code: 'ABC' } }),
            ),
          },
        },
        {
          provide: QUEZAP_SERVICE,
          useValue: {
            duplicateQuezap: jest.fn().mockReturnValue(
              of({ kind: 'success', result: { id: 'q2' } }),
            ),
          },
        },
      ],
    }).compileComponents()

    fixture = TestBed.createComponent(QuezapCard)
    component = fixture.componentInstance
    fixture.componentRef.setInput('quezap', {
      id: '1',
      title: 'Plantes caducs',
      description: 'Test description',
      theme: { id: 't1', title: 'Plantes' },
    })
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

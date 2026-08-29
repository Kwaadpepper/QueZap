import { ComponentFixture, TestBed } from '@angular/core/testing'

import { SessionCard } from './session-card'

describe('SessionCard', () => {
  let component: SessionCard
  let fixture: ComponentFixture<SessionCard>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [SessionCard] })
      .compileComponents()

    fixture = TestBed.createComponent(SessionCard)
    component = fixture.componentInstance
    fixture.componentRef.setInput('session', {
      id: '1',
      name: 'Test Session',
      sessionCode: '123456',
      startedAt: null,
      endedAt: null,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      participants: [],
      quezap: {
        id: 'q1',
        title: 'Test Quezap',
        description: 'Desc',
        theme: { id: 't1', title: 'Theme' },
      },
    })
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

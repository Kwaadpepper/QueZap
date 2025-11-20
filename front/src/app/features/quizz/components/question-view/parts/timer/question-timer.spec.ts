import { ComponentFixture, TestBed } from '@angular/core/testing'

import { QuestionTimer } from './question-timer'

describe('QuestionTimer', () => {
  let component: QuestionTimer
  let fixture: ComponentFixture<QuestionTimer>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [QuestionTimer] })
      .compileComponents()

    fixture = TestBed.createComponent(QuestionTimer)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

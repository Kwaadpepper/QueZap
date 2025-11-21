import { ComponentFixture, TestBed } from '@angular/core/testing'

import { QuestionAlert } from './question-alert'

describe('QuestionAlert', () => {
  let component: QuestionAlert
  let fixture: ComponentFixture<QuestionAlert>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [QuestionAlert] })
      .compileComponents()

    fixture = TestBed.createComponent(QuestionAlert)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

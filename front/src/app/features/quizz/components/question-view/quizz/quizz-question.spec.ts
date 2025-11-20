import { ComponentFixture, TestBed } from '@angular/core/testing'

import { QuizzQuestionView } from './quizz-question'

describe('QuizzQuestionView', () => {
  let component: QuizzQuestionView
  let fixture: ComponentFixture<QuizzQuestionView>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [QuizzQuestionView] })
      .compileComponents()

    fixture = TestBed.createComponent(QuizzQuestionView)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

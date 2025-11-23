import { ComponentFixture, TestBed } from '@angular/core/testing'

import { QuestionListView } from './question-list-view'

describe('QuestionListView', () => {
  let component: QuestionListView
  let fixture: ComponentFixture<QuestionListView>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [QuestionListView] })
      .compileComponents()

    fixture = TestBed.createComponent(QuestionListView)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

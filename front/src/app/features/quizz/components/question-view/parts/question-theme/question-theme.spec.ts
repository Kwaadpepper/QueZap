import { ComponentFixture, TestBed } from '@angular/core/testing'

import { QuestionTheme } from './question-theme'

describe('QuestionTheme', () => {
  let component: QuestionTheme
  let fixture: ComponentFixture<QuestionTheme>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [QuestionTheme] })
      .compileComponents()

    fixture = TestBed.createComponent(QuestionTheme)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

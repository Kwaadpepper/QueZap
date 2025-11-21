import { ComponentFixture, TestBed } from '@angular/core/testing'

import { QuestionIcon } from './question-icon'

describe('QuestionIcon', () => {
  let component: QuestionIcon
  let fixture: ComponentFixture<QuestionIcon>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [QuestionIcon] })
      .compileComponents()

    fixture = TestBed.createComponent(QuestionIcon)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

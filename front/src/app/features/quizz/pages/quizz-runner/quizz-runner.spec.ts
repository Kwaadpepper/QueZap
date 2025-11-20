import { ComponentFixture, TestBed } from '@angular/core/testing'

import { QuizzRunner } from './quizz-runner'

describe('QuizzRunner', () => {
  let component: QuizzRunner
  let fixture: ComponentFixture<QuizzRunner>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [QuizzRunner] })
      .compileComponents()

    fixture = TestBed.createComponent(QuizzRunner)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

import { ComponentFixture, TestBed } from '@angular/core/testing'

import { QuestionAnswer } from './question-answer'

describe('QuestionAnswer', () => {
  let component: QuestionAnswer
  let fixture: ComponentFixture<QuestionAnswer>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [QuestionAnswer] })
      .compileComponents()

    fixture = TestBed.createComponent(QuestionAnswer)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should use unified shapes and form index for standard quizz choices', () => {
    fixture.componentRef.setInput('questionId', '018e3a2b-8888-7def-8123-abcdef888888')
    fixture.componentRef.setInput('answer', { index: 0, value: 'Option A' })
    expect(component['currentForm']()).toBe('circle')
    expect(component['currentFormIndex']()).toBe(0)

    fixture.componentRef.setInput('answer', { index: 1, value: 'Option B' })
    expect(component['currentForm']()).toBe('diamond')
    expect(component['currentFormIndex']()).toBe(1)

    fixture.componentRef.setInput('answer', { index: 2, value: 'Option C' })
    expect(component['currentForm']()).toBe('cross')
    expect(component['currentFormIndex']()).toBe(2)

    fixture.componentRef.setInput('answer', { index: 3, value: 'Option D' })
    expect(component['currentForm']()).toBe('hexagon')
    expect(component['currentFormIndex']()).toBe(3)
  })

  it('should use dedicated shapes and colors for boolean choices', () => {
    fixture.componentRef.setInput('questionId', '018e3a2b-8888-7def-8123-abcdef888888')
    fixture.componentRef.setInput('answer', { index: 0, value: 'Vrai' })
    expect(component['currentForm']()).toBe('circle')
    expect(component['currentFormIndex']()).toBe(3)

    fixture.componentRef.setInput('answer', { index: 1, value: 'Faux' })
    expect(component['currentForm']()).toBe('cross')
    expect(component['currentFormIndex']()).toBe(0)
  })
})

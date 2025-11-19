import { ComponentFixture, TestBed } from '@angular/core/testing'

import { BooleanQuestionView } from './boolean-question'

describe('BooleanQuestionView', () => {
  let component: BooleanQuestionView
  let fixture: ComponentFixture<BooleanQuestionView>

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BooleanQuestionView],
    })
      .compileComponents()

    fixture = TestBed.createComponent(BooleanQuestionView)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

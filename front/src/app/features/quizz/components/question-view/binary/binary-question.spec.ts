import { ComponentFixture, TestBed } from '@angular/core/testing'

import { BinaryQuestionView } from './binary-question'

describe('BinaryQuestionView', () => {
  let component: BinaryQuestionView
  let fixture: ComponentFixture<BinaryQuestionView>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [BinaryQuestionView] })
      .compileComponents()

    fixture = TestBed.createComponent(BinaryQuestionView)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

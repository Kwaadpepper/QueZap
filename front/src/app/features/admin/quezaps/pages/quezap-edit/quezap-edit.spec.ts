import { ComponentFixture, TestBed } from '@angular/core/testing'

import { QuezapEdit } from './quezap-edit'

describe('QuezapEdit', () => {
  let component: QuezapEdit
  let fixture: ComponentFixture<QuezapEdit>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [QuezapEdit] })
      .compileComponents()

    fixture = TestBed.createComponent(QuezapEdit)
    component = fixture.componentInstance
    fixture.componentRef.setInput('quezap', {
      id: '018e3a2b-1111-7def-8123-abcdef111111',
      title: 'Test Quezap',
      description: '',
      questionWithAnswersAndResponses: [],
    })
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

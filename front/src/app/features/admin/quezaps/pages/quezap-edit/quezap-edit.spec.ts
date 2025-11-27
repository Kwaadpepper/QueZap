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
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

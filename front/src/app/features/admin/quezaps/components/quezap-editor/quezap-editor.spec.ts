import { ComponentFixture, TestBed } from '@angular/core/testing'

import { QuezapEditor } from './quezap-editor'

describe('QuezapEditor', () => {
  let component: QuezapEditor
  let fixture: ComponentFixture<QuezapEditor>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [QuezapEditor] })
      .compileComponents()

    fixture = TestBed.createComponent(QuezapEditor)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

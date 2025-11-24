import { ComponentFixture, TestBed } from '@angular/core/testing'

import { TitleEditor } from './title-editor'

describe('TitleEditor', () => {
  let component: TitleEditor
  let fixture: ComponentFixture<TitleEditor>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TitleEditor] })
      .compileComponents()

    fixture = TestBed.createComponent(TitleEditor)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

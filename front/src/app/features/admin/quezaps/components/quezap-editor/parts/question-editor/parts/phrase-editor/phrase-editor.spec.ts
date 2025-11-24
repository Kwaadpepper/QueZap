import { ComponentFixture, TestBed } from '@angular/core/testing'

import { PhraseEditor } from './phrase-editor'

describe('PhraseEditor', () => {
  let component: PhraseEditor
  let fixture: ComponentFixture<PhraseEditor>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [PhraseEditor] })
      .compileComponents()

    fixture = TestBed.createComponent(PhraseEditor)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

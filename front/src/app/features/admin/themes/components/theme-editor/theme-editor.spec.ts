import { ComponentFixture, TestBed } from '@angular/core/testing'

import { ThemeEditor } from './theme-editor'

describe('ThemeEditor', () => {
  let component: ThemeEditor
  let fixture: ComponentFixture<ThemeEditor>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ThemeEditor] })
      .compileComponents()

    fixture = TestBed.createComponent(ThemeEditor)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

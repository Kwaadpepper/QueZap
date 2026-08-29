import { ComponentFixture, TestBed } from '@angular/core/testing'

import { QuezapEditorStore } from '@quezap/features/admin/quezaps/stores'

import { TitleEditor } from './title-editor'

describe('TitleEditor', () => {
  let component: TitleEditor
  let fixture: ComponentFixture<TitleEditor>

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TitleEditor],
      providers: [QuezapEditorStore],
    })
      .compileComponents()

    fixture = TestBed.createComponent(TitleEditor)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})

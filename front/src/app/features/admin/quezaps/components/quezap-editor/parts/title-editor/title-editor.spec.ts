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

  it('should update title via editorStore.updateTitle and set isDirty to true', () => {
    const store = TestBed.inject(QuezapEditorStore)
    store.setQuezap({
      title: 'Ancien titre',
      description: '',
      questionWithAnswersAndResponses: store.questions(),
    })
    expect(store.isDirty()).toBe(false)

    component['openCallback']()
    component['editedTitle'].set('Nouveau titre')
    component['closeCallback']()

    expect(store.quezap().title).toBe('Nouveau titre')
    expect(store.isDirty()).toBe(true)
  })
})

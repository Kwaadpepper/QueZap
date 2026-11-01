import { ComponentFixture, TestBed } from '@angular/core/testing'

import { PictureUrl } from '@quezap/domain/models'

import { QuezapEditorStore } from '../../../../stores'

import { QuestionEditor } from './question-editor'

describe('QuestionEditor', () => {
  let component: QuestionEditor
  let fixture: ComponentFixture<QuestionEditor>
  let store: InstanceType<typeof QuezapEditorStore>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [QuestionEditor] })
      .compileComponents()

    store = TestBed.inject(QuezapEditorStore)
    fixture = TestBed.createComponent(QuestionEditor)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should display picture when selectedQuestion has a picture and remove it on removePicture()', () => {
    const q = store.selectedQuestion()
    store.updateQuestionAtIdx(0, {
      ...q,
      picture: '/quezap-public-images/pictures/questions/test.png' as PictureUrl,
    })
    fixture.detectChanges()

    const img: HTMLImageElement | null = fixture.nativeElement.querySelector('img[alt="Question Image"]')
    expect(img).toBeTruthy()
    expect(img?.getAttribute('src')).toBe('/quezap-public-images/pictures/questions/test.png')

    const removeBtn: HTMLButtonElement | null = fixture.nativeElement.querySelector(
      'button[aria-label="Supprimer l\'image de la question"]',
    )
    expect(removeBtn).toBeTruthy()
    removeBtn?.click()
    fixture.detectChanges()

    expect(store.selectedQuestion().picture).toBeUndefined()
    expect(fixture.nativeElement.querySelector('img[alt="Question Image"]')).toBeNull()
  })
})

import { ComponentFixture, TestBed } from '@angular/core/testing'

import { MessageService } from 'primeng/api'
import { of } from 'rxjs'

import { ValidationError } from '@quezap/core/errors'
import { PictureUploadService } from '@quezap/core/services/picture-upload/picture-upload'
import { PictureUrl } from '@quezap/domain/models'

import { QuezapEditorStore } from '../../../../stores'

import { QuestionEditor } from './question-editor'

describe('QuestionEditor', () => {
  let component: QuestionEditor
  let fixture: ComponentFixture<QuestionEditor>
  let store: InstanceType<typeof QuezapEditorStore>

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuestionEditor],
      providers: [MessageService],
    }).compileComponents()

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

  it('should upload picture via PictureUploadService and update store', () => {
    const uploadService = TestBed.inject(PictureUploadService)
    const mockFile = new File(['image-bytes'], 'quiz.png', { type: 'image/png' })
    const uploadedUrl = '/quezap-public-images/pictures/questions/uploaded.png' as PictureUrl

    jest.spyOn(uploadService, 'upload').mockReturnValue(
      of({
        kind: 'success' as const,
        result: {
          pictureUrl: uploadedUrl,
          objectKey: 'pictures/questions/uploaded.png',
          expiresAt: '2026-11-02T12:00:00Z',
        },
      }),
    )

    component['onUpload']({ files: [mockFile], originalEvent: new Event('change') })
    fixture.detectChanges()

    expect(store.selectedQuestion().picture).toBe(uploadedUrl)
  })

  it('should show error toast and stop spinner when upload fails', () => {
    const uploadService = TestBed.inject(PictureUploadService)
    const messageService = TestBed.inject(MessageService)
    const toastSpy = jest.spyOn(messageService, 'add')
    const mockFile = new File(['bad'], 'bad.png', { type: 'image/png' })

    jest.spyOn(uploadService, 'upload').mockReturnValue(
      of({
        kind: 'failure' as const,
        error: new ValidationError({}, 'Format non supporté'),
      }),
    )

    component['onUpload']({ files: [mockFile], originalEvent: new Event('change') })
    fixture.detectChanges()

    expect(component['isUploading']()).toBe(false)
    expect(toastSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        detail: 'Format non supporté',
      }),
    )
  })
})

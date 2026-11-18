import { ComponentFixture, TestBed } from '@angular/core/testing'

import { of } from 'rxjs'

import { PictureUploadService } from '@quezap/core/services/picture-upload/picture-upload'
import { AnswerWithResponse, PictureUrl } from '@quezap/domain/models'

import { AnswerComponent, AnswerInput } from './answer'

describe('AnswerComponent', () => {
  let component: AnswerComponent
  let fixture: ComponentFixture<AnswerComponent>
  let uploadService: PictureUploadService

  const initialAnswer: AnswerInput = {
    index: 0,
    value: 'Paris',
    isCorrect: true,
    points: 1000,
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnswerComponent],
      providers: [PictureUploadService],
    }).compileComponents()

    uploadService = TestBed.inject(PictureUploadService)
    fixture = TestBed.createComponent(AnswerComponent)
    component = fixture.componentInstance

    fixture.componentRef.setInput('answer', initialAnswer)
    fixture.componentRef.setInput('readonly', false)
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should emit updated answer when removing picture', () => {
    const answerWithPic: AnswerInput = {
      ...initialAnswer,
      picture: '/quezap-public-images/pictures/questions/ans.png' as PictureUrl,
    }
    fixture.componentRef.setInput('answer', answerWithPic)
    fixture.detectChanges()

    let emitted: AnswerWithResponse | undefined
    component.answerChanged.subscribe((val) => {
      emitted = val
    })

    component['removePicture']()

    expect(emitted).toBeDefined()
    expect(emitted?.picture).toBeUndefined()
  })

  it('should upload picture via PictureUploadService and emit updated answer', () => {
    const mockFile = new File(['answer-image'], 'flag.png', { type: 'image/png' })
    const uploadedUrl = '/quezap-public-images/pictures/questions/flag.png' as PictureUrl

    jest.spyOn(uploadService, 'upload').mockReturnValue(
      of({
        kind: 'success' as const,
        result: {
          pictureUrl: uploadedUrl,
          objectKey: 'pictures/questions/flag.png',
          expiresAt: '2026-11-02T12:00:00Z',
        },
      }),
    )

    let emitted: AnswerWithResponse | undefined
    component.answerChanged.subscribe((val) => {
      emitted = val
    })

    component['onUpload']({ files: [mockFile], originalEvent: new Event('change') })

    expect(emitted).toBeDefined()
    expect(emitted?.picture).toBe(uploadedUrl)
  })
})

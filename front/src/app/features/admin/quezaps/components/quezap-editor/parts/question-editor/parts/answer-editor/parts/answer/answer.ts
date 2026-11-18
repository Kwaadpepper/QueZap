import {
  ChangeDetectionStrategy, Component, computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core'

import { FileSelectEvent, FileUploadModule } from 'primeng/fileupload'

import { PictureUploadService } from '@quezap/core/services/picture-upload/picture-upload'
import { isSuccess } from '@quezap/core/types'
import { AnswerWithResponse } from '@quezap/domain/models'
import { QuestionIcon, QuestionIconType } from '@quezap/features/quizz/components/question-view/parts'

import { AnswerCorrectnessComponent } from '../answer-correctness/answer-correctness'
import { PhraseInput } from '../phrase-input/phrase-input'

export type AnswerInput = AnswerWithResponse

@Component({
  selector: 'quizz-answer',
  templateUrl: './answer.html',
  imports: [
    QuestionIcon,
    PhraseInput,
    AnswerCorrectnessComponent,
    FileUploadModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnswerComponent {
  private readonly pictureUploadService = inject(PictureUploadService)

  readonly answer = input.required<AnswerInput>()
  readonly readonly = input.required<boolean>()
  readonly canDelete = input<boolean>(false)
  readonly answerChanged = output<AnswerInput>()
  readonly deleteAnswer = output<number>()

  protected readonly isUploading = signal(false)
  protected readonly index = computed(() => this.answer().index)
  protected readonly phrase = computed(() => this.answer().value ?? '')
  protected readonly isCorrect = computed(() => this.answer().isCorrect)

  protected readonly isBooleanChoice = computed<boolean>(() => {
    const text = this.phrase().trim().toLowerCase()
    return text === 'vrai' || text === 'faux'
  })

  protected readonly currentFormIndex = computed<number>(() => {
    if (this.isBooleanChoice()) {
      return this.phrase().trim().toLowerCase() === 'vrai' ? 3 : 0
    }
    return this.index() % 4
  })

  protected readonly currentForm = computed<QuestionIconType>(() => {
    if (this.isBooleanChoice()) {
      return this.phrase().trim().toLowerCase() === 'vrai'
        ? QuestionIconType.Circle
        : QuestionIconType.Cross
    }
    const forms = [
      QuestionIconType.Circle,
      QuestionIconType.Diamond,
      QuestionIconType.Cross,
      QuestionIconType.Hexagon,
    ]
    return forms[this.index() % forms.length]
  })

  protected onPhraseChanged(newPhrase: string) {
    this.answerChanged.emit({
      ...this.answer(),
      value: newPhrase,
    })
  }

  protected onCorrectnessChanged(isCorrect: boolean) {
    this.answerChanged.emit({
      ...this.answer(),
      isCorrect,
    })
  }

  protected onUpload(event: FileSelectEvent) {
    const file = event.files[0]
    if (file) {
      this.isUploading.set(true)
      this.pictureUploadService.upload(file).subscribe({
        next: (output) => {
          this.isUploading.set(false)
          if (isSuccess(output)) {
            this.answerChanged.emit({
              ...this.answer(),
              picture: output.result.pictureUrl,
            })
          }
        },
        error: () => {
          this.isUploading.set(false)
        },
      })
    }
  }

  protected removePicture() {
    this.answerChanged.emit({
      ...this.answer(),
      picture: undefined,
    })
  }
}

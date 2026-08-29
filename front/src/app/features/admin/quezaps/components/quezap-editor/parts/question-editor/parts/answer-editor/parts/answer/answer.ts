import {
  ChangeDetectionStrategy, Component, computed,
  input,
  output,
} from '@angular/core'

import { FileSelectEvent, FileUploadModule } from 'primeng/fileupload'

import { AnswerWithResponse } from '@quezap/domain/models'
import { PictureUrl } from '@quezap/domain/models'
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
  readonly answer = input.required<AnswerInput>()
  readonly readonly = input.required<boolean>()
  readonly answerChanged = output<AnswerInput>()

  protected readonly index = computed(() => this.answer().index)
  protected readonly phrase = computed(() => this.answer().value ?? '')
  protected readonly currentForm = computed(() => Object.values(QuestionIconType)[this.index()])
  protected readonly isCorrect = computed(() => this.answer().isCorrect)

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
      const reader = new FileReader()
      reader.onload = (e) => {
        const pictureUrl = (e.target?.result as string) as PictureUrl
        this.answerChanged.emit({
          ...this.answer(),
          picture: pictureUrl,
        })
      }
      reader.readAsDataURL(file)
    }
  }

  protected removePicture() {
    this.answerChanged.emit({
      ...this.answer(),
      picture: undefined,
    })
  }
}

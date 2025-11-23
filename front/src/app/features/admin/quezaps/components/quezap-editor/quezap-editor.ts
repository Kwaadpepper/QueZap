import {
  ChangeDetectionStrategy, Component,
  effect,
  input,
  signal,
} from '@angular/core'

import {
  QuestionType, QuestionTypeFrom, QuestionWithAnswers,
  QuezapWithQuestionsAndAnswers,
} from '@quezap/domain/models'

import { QuestionEditor, QuestionListView } from './parts'

export type QuezapEditorInput = Omit<QuezapWithQuestionsAndAnswers, 'id'>

@Component({
  selector: 'quizz-quezap-editor',
  imports: [QuestionListView, QuestionEditor],
  templateUrl: './quezap-editor.html',
  styles: ':host { display: block; height: 100%; width: 100%; }',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuezapEditor {
  readonly quezap = input.required<QuezapEditorInput>()

  protected readonly selectedIdx = signal<number>(0)
  protected readonly selectedQuestion = signal<QuestionWithAnswers>(
    // * Initial question to avoid empty state
    QuestionTypeFrom(QuestionType.Quizz)
      .getNewWithAnswers(),
  )

  protected readonly questions = signal<QuestionWithAnswers[]>([
    // * Initial question to avoid empty state
    this.selectedQuestion(),
  ])

  constructor() {
    effect(() => {
      const input = this.quezap()
      this.onQuezapChanged(input)
    }, { debugName: 'Quezap changed' })

    effect(() => {
      const idx = this.selectedIdx()
      this.onQuestionSelected(idx)
    }, { debugName: 'Selected question idx' })

    effect(() => {
      const question = this.selectedQuestion()
      this.onQuestionUpdated(question)
    }, { debugName: 'Selected question changed' })
  }

  private onQuezapChanged(input: QuezapEditorInput) {
    const questions = input.questionWithAnswersAndResponses
    this.questions.set(
      questions.length
        ? questions
        : [
            QuestionTypeFrom(QuestionType.Quizz)
              .getNewWithAnswers(),
          ],
    )
  }

  private onQuestionSelected(idx: number) {
    const question = this.questions()[idx]

    if (!question) {
      return
    }

    this.selectedIdx.set(idx)
    this.selectedQuestion.set(question)
  }

  private onQuestionUpdated(updated: QuestionWithAnswers) {
    this.questions.update((questions) => {
      const idx = this.selectedIdx()

      questions[idx] = updated
      return [...questions]
    })
  }
}

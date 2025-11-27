import {
  ChangeDetectionStrategy, Component,
  computed,
  effect,
  inject,
  model,
  output,
  signal,
} from '@angular/core'

import { ConfirmationService } from 'primeng/api'
import { ButtonModule } from 'primeng/button'
import { ConfirmDialogModule } from 'primeng/confirmdialog'

import {
  QuestionType, QuestionTypeFrom,
  QuestionWithAnswersAndResponses,
  QuezapWithQuestionsAndAnswers,
} from '@quezap/domain/models'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'

import { QuezapEditorContainer } from './editor-container'
import { QuestionEditor, QuestionListView, TitleEditor } from './parts'

export type QuezapEditorInput = Omit<QuezapWithQuestionsAndAnswers, 'id'>

@Component({
  selector: 'quizz-quezap-editor',
  imports: [
    QuestionListView,
    QuestionEditor,
    TitleEditor,
    ButtonModule,
    ConfirmDialogModule,
    IconFacade,
  ],
  providers: [
    ConfirmationService,
    QuezapEditorContainer,
  ],
  templateUrl: './quezap-editor.html',
  styles: ':host { display: block; height: 100%; width: 100%; }',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuezapEditor {
  private readonly editorContainer = inject(QuezapEditorContainer)
  private readonly confirmation = inject(ConfirmationService)

  readonly quezap = model.required<QuezapEditorInput>()

  readonly closeEvent = output<void>()
  readonly saveEvent = output<void>()

  protected readonly isDirty = computed(() => this.editorContainer.isDirty())
  private readonly selectedIdx = signal<number>(
    this.editorContainer.selectionQuestionIdx(),
  )

  protected readonly selectedQuestion = signal<QuestionWithAnswersAndResponses>(
    // * Initial question to avoid empty state
    QuestionTypeFrom(QuestionType.Quizz)
      .getNewWithAnswers(),
  )

  protected readonly questions = signal<QuestionWithAnswersAndResponses[]>([
    // * Initial question to avoid empty state
    this.selectedQuestion(),
  ])

  constructor() {
    effect(() => {
      const input = this.editorContainer.quezap()
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

  protected onSaveQuezap() {
    this.saveEvent.emit()
    this.editorContainer.isDirty.set(false)
  }

  protected onCloseEditor() {
    if (this.isDirty()) {
      this.confirmation.confirm({
        message: 'Voulez-vous quitter sans enregistrer les modifications ?',
        header: 'Confirmation',
        acceptButtonStyleClass: 'p-button-danger p-button-text',
        acceptLabel: 'Oui',
        rejectLabel: 'Non',
        accept: () => {
          this.closeEvent.emit()
        },
      })
      return
    }

    this.closeEvent.emit()
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

  private onQuestionUpdated(updated: QuestionWithAnswersAndResponses) {
    this.questions.update((questions) => {
      const idx = this.selectedIdx()

      questions[idx] = updated

      this.quezap.update(q => ({
        ...q,
        questionWithAnswersAndResponses: [...questions],
      }))
      return [...questions]
    })
  }
}

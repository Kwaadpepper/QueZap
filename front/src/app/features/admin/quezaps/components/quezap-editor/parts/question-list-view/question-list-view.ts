import {
  ChangeDetectionStrategy, Component,
  computed,
  effect,
  ElementRef,
  inject,
  viewChild,
} from '@angular/core'

import { ConfirmationService } from 'primeng/api'
import { BadgeModule } from 'primeng/badge'
import { ButtonModule } from 'primeng/button'
import { FocusTrapModule } from 'primeng/focustrap'

import { scrollToElementInContainer } from '@quezap/core/tools/scroll-to'
import { QuestionTypeFrom, QuestionWithAnswersAndResponses } from '@quezap/domain/models'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'

import { QuezapEditorContainer } from '../../editor-container'

import { QuestionActionBar, QuestionPreviewButton } from './parts'

export type QuestionListViewInput = Omit<QuestionWithAnswersAndResponses, 'id'>[]

@Component({
  selector: 'quizz-question-list-view',
  imports: [
    ButtonModule,
    FocusTrapModule,
    IconFacade,
    BadgeModule,
    QuestionPreviewButton,
    QuestionActionBar,
  ],
  providers: [
    ConfirmationService,
  ],
  templateUrl: './question-list-view.html',
  styleUrl: './question-list-view.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionListView {
  private readonly editorContainer = inject(QuezapEditorContainer)

  readonly questions = computed<QuestionListViewInput>(() =>
    this.editorContainer.quezap().questionWithAnswersAndResponses,
  )

  readonly selectedIdx = computed(() =>
    this.editorContainer.selectionQuestionIdx(),
  )

  protected readonly QuestionTypeFrom = QuestionTypeFrom

  protected readonly questionContainer = viewChild.required<ElementRef<HTMLDivElement>>('questionContainer')

  constructor() {
    effect(() => {
      const selectedIdx = this.selectedIdx()
      this.scrollToQuestion(selectedIdx)
    }, { debugName: 'Scroll to selected question' })
  }

  protected onSelectQuestion(index: number) {
    this.selectQuestion(index)
  }

  protected onAddQuestion() {
    this.editorContainer.addNewQuestion()
  }

  protected onKeyDown(event: KeyboardEvent) {
    const index = this.selectedIdx()
    if (event.key === 'ArrowDown' || event.key === 'Down') {
      event.preventDefault()
      const nextIndex = Math.min(this.questions().length - 1, index + 1)

      this.selectQuestion(nextIndex)
    }
    if (event.key === 'ArrowUp' || event.key === 'Up') {
      event.preventDefault()
      const prevIndex = Math.max(0, index - 1)

      this.selectQuestion(prevIndex)
    }
  }

  private selectQuestion(index: number) {
    this.editorContainer.setSelectionQuestionIdx(index)
  }

  private scrollToQuestion(index: number) {
    setTimeout(() => {
      const container = this.questionContainer().nativeElement
      const selectedQuestionElement = container.children.item(index) as HTMLElement | null

      if (!selectedQuestionElement) {
        return
      }

      const scrollPadding = 80

      scrollToElementInContainer(
        container,
        selectedQuestionElement,
        scrollPadding,
      )
    }, 0)
  }
}

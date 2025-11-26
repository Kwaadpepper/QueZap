import {
  ChangeDetectionStrategy, Component,
  computed,
  effect,
  inject,
  ViewChild,
} from '@angular/core'

import { ConfirmationService } from 'primeng/api'
import { ButtonModule } from 'primeng/button'
import { ConfirmPopup } from 'primeng/confirmpopup'
import { FocusTrapModule } from 'primeng/focustrap'
import { Tag } from 'primeng/tag'

import { scrollToElementInContainer } from '@quezap/core/tools/scroll-to'
import { QuestionTypeFrom, QuestionWithAnswers } from '@quezap/domain/models'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'
import { MinutesPipe } from '@quezap/shared/pipes/minutes'

import { QuezapEditorContainer } from '../../editor-container'

export type QuestionListViewInput = Omit<QuestionWithAnswers, 'id'>[]

@Component({
  selector: 'quizz-question-list-view',
  imports: [
    ButtonModule,
    Tag,
    ConfirmPopup,
    FocusTrapModule,
    MinutesPipe,
    IconFacade,
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
  private readonly confirmationService = inject(ConfirmationService)

  readonly questions = computed<QuestionListViewInput>(() =>
    this.editorContainer.quezap().questionWithAnswersAndResponses,
  )

  readonly selectedIdx = computed(() =>
    this.editorContainer.selectionQuestionIdx(),
  )

  protected readonly QuestionTypeFrom = QuestionTypeFrom

  @ViewChild('questionContainer')
  protected readonly questionContainer!: { nativeElement: HTMLDivElement }

  @ViewChild('deleteQuestionCancelButton')
  protected readonly deleteQuestionCancelButton!: { nativeElement: HTMLButtonElement } | undefined

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

  protected onDuplicateQuestion(index: number) {
    this.editorContainer.duplicateQuestionAtIdx(index)
  }

  protected onDeleteQuestion($event: Event, index: number) {
    this.confirmationService.confirm({
      target: $event.currentTarget ?? undefined,
      blockScroll: true,
      rejectButtonProps: {
        severity: 'primary',
        outlined: true,
      },
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.editorContainer.deleteQuestionAtIdx(index)
      },
    })
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
      const container = this.questionContainer.nativeElement
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

import {
  ChangeDetectionStrategy, Component,
  computed,
  inject,
  ViewChild,
} from '@angular/core'

import { ConfirmationService } from 'primeng/api'
import { ButtonModule } from 'primeng/button'
import { ConfirmPopup } from 'primeng/confirmpopup'
import { FocusTrapModule } from 'primeng/focustrap'
import { Tag } from 'primeng/tag'

import { scrollToElementInContainer } from '@quezap/core/tools/scroll-to'
import { QuestionType, QuestionTypeFrom, QuestionWithAnswers } from '@quezap/domain/models'
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

  protected onSelectQuestion(index: number) {
    this.selectQuestion(index)
  }

  protected onAddQuestion() {
    this.questions().push(
      QuestionTypeFrom(QuestionType.Quizz)
        .getNewWithAnswers(),
    )
    this.selectQuestion(this.questions().length - 1)
  }

  protected onDuplicateQuestion(index: number) {
    const questionToDuplicate = this.questions()[index]
    const duplicatedQuestion: QuestionListViewInput[0] = {
      ...questionToDuplicate,
      answers: questionToDuplicate.answers.map(answer => ({ ...answer })),
    }

    const newIndex = index + 1
    this.questions().splice(newIndex, 0, duplicatedQuestion)
    this.selectQuestion(newIndex)
  }

  protected onDeleteQuestion($event: Event, index: number) {
    if (this.questions().length <= 1) {
      throw new Error('At least one question must exist')
    }

    this.confirmationService.confirm({
      target: $event.currentTarget ?? undefined,
      blockScroll: true,
      rejectButtonProps: {
        severity: 'primary',
        outlined: true,
      },
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.questions().splice(index, 1)
        const newIndex = Math.min(
          this.selectedIdx(),
          this.questions().length - 1,
        )
        this.selectQuestion(newIndex)
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
    this.scrollToQuestion(index)
  }

  private scrollToQuestion(index: number) {
    setTimeout(() => {
      const container = this.questionContainer.nativeElement
      const selectedQuestionElement = container.children.item(index) as HTMLElement | null

      if (!selectedQuestionElement) {
        return
      }

      const scrollPadding = 50

      scrollToElementInContainer(
        container,
        selectedQuestionElement,
        scrollPadding,
      )
    }, 0)
  }
}

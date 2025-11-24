import {
  ChangeDetectionStrategy, Component,
  inject,
  input, model,
  ViewChild,
} from '@angular/core'

import { ConfirmationService } from 'primeng/api'
import { ButtonDirective } from 'primeng/button'
import { ConfirmPopup } from 'primeng/confirmpopup'
import { FocusTrapModule } from 'primeng/focustrap'
import { Tag } from 'primeng/tag'

import { scrollToElementInContainer } from '@quezap/core/tools'
import { QuestionType, QuestionTypeFrom, QuestionWithAnswers } from '@quezap/domain/models'
import { MinutesPipe } from '@quezap/shared/pipes'

export type QuestionListViewInput = Omit<QuestionWithAnswers, 'id'>[]

@Component({
  selector: 'quizz-question-list-view',
  imports: [
    ButtonDirective,
    Tag,
    ConfirmPopup,
    FocusTrapModule,
    MinutesPipe,
  ],
  providers: [
    ConfirmationService,
  ],
  templateUrl: './question-list-view.html',
  styleUrl: './question-list-view.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionListView {
  private readonly confirmationService = inject(ConfirmationService)

  readonly questions = input.required<QuestionListViewInput>()
  readonly selectedIdx = model<number>(0)

  protected readonly QuestionTypeFrom = QuestionTypeFrom

  @ViewChild('questionContainer')
  protected readonly questionContainer!: { nativeElement: HTMLDivElement }

  @ViewChild('deleteQuestionCancelButton')
  protected readonly deleteQuestionCancelButton!: { nativeElement: HTMLButtonElement } | undefined

  protected onSelectQuestion(index: number) {
    this.selectedIdx.set(index)
    this.scrollToQuestion(index)
  }

  protected onAddQuestion() {
    this.questions().push(
      QuestionTypeFrom(QuestionType.Quizz)
        .getNewWithAnswers(),
    )
    this.onSelectQuestion(this.questions().length - 1)
    this.scrollToQuestion(this.questions().length - 1)
  }

  protected onDuplicateQuestion(index: number) {
    const questionToDuplicate = this.questions()[index]
    const duplicatedQuestion: QuestionListViewInput[0] = {
      ...questionToDuplicate,
      answers: questionToDuplicate.answers.map(answer => ({ ...answer })),
    }

    this.questions().splice(index + 1, 0, duplicatedQuestion)
    this.onSelectQuestion(index + 1)
    this.scrollToQuestion(index + 1)
  }

  protected onDeleteQuestion($event: Event, index: number) {
    if (this.questions().length <= 1) {
      throw new Error('At least one question must exist')
    }

    this.confirmationService.confirm({
      target: $event.currentTarget ?? undefined,
      icon: 'pi pi-trash',
      rejectIcon: 'pi pi-times',
      rejectLabel: ' ',
      acceptIcon: 'pi pi-check',
      acceptLabel: ' ',
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
        this.onSelectQuestion(newIndex)
        this.scrollToQuestion(newIndex)
      },
      reject: () => {
        console.log('Deletion cancelled')
      },
    })
  }

  protected onKeyDown(event: KeyboardEvent) {
    const index = this.selectedIdx()
    if (event.key === 'ArrowDown' || event.key === 'Down') {
      event.preventDefault()
      const nextIndex = Math.min(this.questions().length - 1, index + 1)
      console.log('nextIndex', index, nextIndex)
      this.onSelectQuestion(nextIndex)
    }
    if (event.key === 'ArrowUp' || event.key === 'Up') {
      event.preventDefault()
      const prevIndex = Math.max(0, index - 1)
      console.log('prevIndex', index, prevIndex)
      this.onSelectQuestion(prevIndex)
    }
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

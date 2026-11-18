import { DestroyRef, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'

import { Subscription } from 'rxjs'

import { isSuccess } from '@quezap/core/types'
import { QuestionId } from '@quezap/domain/models'
import { SESSION_API_SERVICE } from '@quezap/features/quizz/services'
import { ActiveSessionStore } from '@quezap/features/quizz/stores'

export interface AnswerSubmissionRequest {
  readonly questionId: QuestionId
  readonly answerIndex?: number
  readonly textAnswer?: string
  readonly choiceLabel: string
  readonly onRollback: () => void
  readonly onSuccess?: () => void
}

export function createAnswerSubmissionState() {
  const sessionStore = inject(ActiveSessionStore)
  const sessionApi = inject(SESSION_API_SERVICE)
  const destroyRef = inject(DestroyRef)

  const hasSubmitted = signal(false)
  const isSubmitting = signal(false)
  const submissionError = signal<string | null>(null)

  let activeSubscription: Subscription | undefined

  destroyRef.onDestroy(() => {
    activeSubscription?.unsubscribe()
  })

  const resetForQuestion = (restoredSubmitted: boolean): void => {
    activeSubscription?.unsubscribe()
    activeSubscription = undefined
    isSubmitting.set(false)
    submissionError.set(null)
    hasSubmitted.set(restoredSubmitted)
  }

  const submit = (request: AnswerSubmissionRequest): void => {
    const previousChoice = sessionStore.mySubmittedChoice()
    const previousHasSubmitted = hasSubmitted() && previousChoice?.questionId === request.questionId

    activeSubscription?.unsubscribe()
    submissionError.set(null)
    isSubmitting.set(true)

    activeSubscription = sessionApi.submitAnswer(request.answerIndex, request.textAnswer).pipe(
      takeUntilDestroyed(destroyRef),
    ).subscribe({
      next: (result) => {
        isSubmitting.set(false)
        if (isSuccess(result)) {
          hasSubmitted.set(true)
          sessionStore.submitMyChoice(
            request.answerIndex ?? 0,
            request.choiceLabel,
            request.questionId,
          )
          request.onSuccess?.()
          return
        }

        hasSubmitted.set(previousHasSubmitted)
        request.onRollback()
        submissionError.set(result.error.message || 'Impossible d\'enregistrer ta réponse. Réessaie.')
      },
      error: () => {
        isSubmitting.set(false)
        hasSubmitted.set(previousHasSubmitted)
        request.onRollback()
        submissionError.set('Connexion perdue : impossible d\'envoyer ta réponse. Vérifie ton réseau et réessaie.')
      },
    })
  }

  return {
    hasSubmitted: hasSubmitted.asReadonly(),
    isSubmitting: isSubmitting.asReadonly(),
    submissionError: submissionError.asReadonly(),
    resetForQuestion,
    submit,
  }
}

import { inject, Injectable } from '@angular/core'

import { catchError, from, map, of } from 'rxjs'

import {
  CreateQuezapRequest,
  InlineAnswerRequest,
  InlineQuestionRequest,
  PageOfDtoQuezapSummaryDto,
  QuezapDetailDto,
  QuezapIdDto,
  QuezapSlideRequest,
  UpdateQuezapRequest,
} from '@quezap/core/api-client'
import { client } from '@quezap/core/api-client/client.gen'
import { NotFoundError, ValidationError } from '@quezap/core/errors'
import { PageOf, Pagination, ServiceOutput, toPageBasedPagination } from '@quezap/core/types'
import {
  PictureUrl, QuestionId, QuestionType, QuestionWithAnswersAndResponses, Quezap, QuezapId,
  QuezapWithQuestionsAndAnswers, QuezapWithTheme, Theme, ThemeId, UserId,
} from '@quezap/domain/models'
import { AuthenticatedUserStore } from '@quezap/shared/stores/authenticated-user'

import { QuezapService } from './quezap'

@Injectable({ providedIn: 'root' })
export class QuezapHttpService implements QuezapService {
  private readonly authStore = inject(AuthenticatedUserStore)

  getQuezapPage(page: Pagination): ServiceOutput<PageOf<QuezapWithTheme>> {
    const p = toPageBasedPagination(page)
    const authorId = this.authStore.currentUser()?.id

    return from(
      client.get<{ 200: PageOfDtoQuezapSummaryDto }>({
        url: '/apiv1/quezaps',
        query: {
          page: p.page,
          pageSize: p.pageSize,
          ...(authorId ? { authorId } : {}),
        },
      }),
    ).pipe(
      map(({ data, error }) => {
        if (error || !data) {
          return {
            kind: 'failure' as const,
            error: new NotFoundError('Erreur lors du chargement des quezaps'),
          }
        }

        return {
          kind: 'success' as const,
          result: {
            data: (data.data ?? []).map(q => ({
              id: (q.id ?? '') as QuezapId,
              title: q.title ?? '',
              description: q.description || '',
              authorId: q.authorId ? (q.authorId as UserId) : undefined,
              totalQuestions: q.totalQuestions ?? 0,
              theme: {
                id: '018e3a2b-8888-7def-8123-abcdef888888' as ThemeId,
                name: 'Général',
              } as Theme,
            })),
            totalElements: data.totalElements ?? 0,
            totalPages: data.totalPages ?? 0,
            page: data.page ?? 0,
            pageSize: data.pageSize ?? p.pageSize,
            hasNext: data.hasNext ?? false,
            hasPrevious: data.hasPrevious ?? false,
          },
        }
      }),
      catchError((error: unknown) =>
        of({
          kind: 'failure' as const,
          error: new NotFoundError(
            error instanceof Error ? error.message : 'Erreur lors du chargement des quezaps',
          ),
        }),
      ),
    )
  }

  find(quezapId: QuezapId): ServiceOutput<QuezapWithQuestionsAndAnswers, NotFoundError> {
    return from(
      client.get<{ 200: QuezapDetailDto }>({ url: `/apiv1/quezaps/${quezapId}` }),
    ).pipe(
      map(({ data, error }) => {
        if (error || !data) {
          return {
            kind: 'failure' as const,
            error: new NotFoundError('Quezap introuvable'),
          }
        }

        const slides = data.slides ?? []

        return {
          kind: 'success' as const,
          result: {
            id: (data.id ?? quezapId) as QuezapId,
            title: data.title ?? '',
            description: data.description || '',
            questionWithAnswersAndResponses: slides.map((s) => {
              const type = (s.question?.type as QuestionType) || QuestionType.Quizz
              const rawAnswers = s.question?.answers || []
              const points = s.points || 1000
              const answers = type === QuestionType.Boolean && rawAnswers.length === 1
                ? [
                    {
                      index: 0,
                      points,
                      value: 'Vrai',
                      isCorrect: Boolean(rawAnswers[0].isCorrect),
                    },
                    {
                      index: 1,
                      points,
                      value: 'Faux',
                      isCorrect: !rawAnswers[0].isCorrect,
                    },
                  ]
                : rawAnswers.map((a, idx) => ({
                    index: idx,
                    points,
                    value: a.label ?? '',
                    isCorrect: a.isCorrect ?? false,
                    ...(a.pictureUrl ? { picture: a.pictureUrl as PictureUrl } : {}),
                  }))

              return {
                id: (s.question?.id ?? '') as QuestionId,
                value: s.question?.label ?? '',
                type,
                ...(s.question?.pictureUrl ? { picture: s.question.pictureUrl as PictureUrl } : {}),
                limit: { seconds: s.timer || 30 },
                answers,
              }
            }),
          },
        }
      }),
      catchError((error: unknown) =>
        of({
          kind: 'failure' as const,
          error: new NotFoundError(
            error instanceof Error ? error.message : 'Quezap introuvable',
          ),
        }),
      ),
    )
  }

  persistQuezap(
    quezap: QuezapWithQuestionsAndAnswers | (Omit<QuezapWithQuestionsAndAnswers, 'id'> & { id?: QuezapId }),
  ): ServiceOutput<Pick<Quezap, 'id'>> {
    const slides = this.buildSlidesPayload(quezap.questionWithAnswersAndResponses)

    if (quezap.id && String(quezap.id).length > 0) {
      const payload: UpdateQuezapRequest = {
        title: quezap.title,
        description: quezap.description || '',
        isPublic: true,
        slides,
      }

      return from(
        client.put<void>({
          url: `/apiv1/quezaps/${quezap.id}`,
          body: payload,
        }),
      ).pipe(
        map(({ error }) => {
          if (error) {
            return {
              kind: 'failure' as const,
              error: new ValidationError({}, 'Erreur lors de la mise à jour du Quezap'),
            }
          }

          return {
            kind: 'success' as const,
            result: { id: quezap.id as QuezapId },
          }
        }),
        catchError((error: unknown) =>
          of({
            kind: 'failure' as const,
            error: new ValidationError(
              {},
              error instanceof Error ? error.message : 'Erreur lors de la mise à jour du Quezap',
            ),
          }),
        ),
      )
    }

    const payload: CreateQuezapRequest = {
      title: quezap.title,
      description: quezap.description || '',
      isPublic: true,
      slides,
      questions: slides,
    }

    return from(
      client.post<{ 200: QuezapIdDto, 201: QuezapIdDto }>({
        url: '/apiv1/quezaps',
        body: payload,
      }),
    ).pipe(
      map(({ data, error }) => {
        if (error || !data?.id) {
          return {
            kind: 'failure' as const,
            error: new ValidationError({}, 'Erreur lors de la sauvegarde du Quezap'),
          }
        }

        return {
          kind: 'success' as const,
          result: { id: data.id as QuezapId },
        }
      }),
      catchError((error: unknown) =>
        of({
          kind: 'failure' as const,
          error: new ValidationError(
            {},
            error instanceof Error ? error.message : 'Erreur lors de la sauvegarde du Quezap',
          ),
        }),
      ),
    )
  }

  private buildSlidesPayload(questions: QuestionWithAnswersAndResponses[]): QuezapSlideRequest[] {
    return questions.map((q) => {
      const answers: InlineAnswerRequest[] = q.answers.map((a, aIdx) => ({
        label: a.value || `Option ${aIdx + 1}`,
        isCorrect: a.isCorrect,
        ...(a.picture ? { pictureFilename: a.picture } : {}),
        answerOrder: aIdx + 1,
      }))

      const inlineQuestion: InlineQuestionRequest = {
        id: q.id && String(q.id).length > 0 ? q.id : undefined,
        label: q.value,
        type: q.type || 'QUIZZ',
        ...(q.picture ? { pictureFilename: q.picture } : {}),
        answers,
      }

      return {
        timer: q.limit?.seconds || 30,
        timerSeconds: q.limit?.seconds || 30,
        points: q.answers[0]?.points || 1000,
        question: inlineQuestion,
        ...(q.id && String(q.id).length > 0 ? { questionId: q.id } : {}),
      }
    })
  }

  duplicateQuezap(quezapId: QuezapId): ServiceOutput<Pick<Quezap, 'id'>> {
    return from(
      client.post<{ 200: QuezapIdDto, 201: QuezapIdDto }>({
        url: `/apiv1/quezaps/${quezapId}/duplicate`,
        body: {},
      }),
    ).pipe(
      map(({ data, error }) => {
        if (error || !data?.id) {
          return {
            kind: 'failure' as const,
            error: new ValidationError({}, 'Erreur lors de la duplication du Quezap'),
          }
        }

        return {
          kind: 'success' as const,
          result: { id: data.id as QuezapId },
        }
      }),
      catchError((error: unknown) =>
        of({
          kind: 'failure' as const,
          error: new ValidationError(
            {},
            error instanceof Error ? error.message : 'Erreur lors de la duplication du Quezap',
          ),
        }),
      ),
    )
  }

  deleteQuezap(quezapId: QuezapId): ServiceOutput<void> {
    return from(
      client.delete<void>({ url: `/apiv1/quezaps/${quezapId}` }),
    ).pipe(
      map(({ error }) => {
        if (error) {
          return {
            kind: 'failure' as const,
            error: new ValidationError({}, 'Erreur lors de la suppression du Quezap'),
          }
        }

        return {
          kind: 'success' as const,
          result: undefined,
        }
      }),
      catchError((error: unknown) =>
        of({
          kind: 'failure' as const,
          error: new ValidationError(
            {},
            error instanceof Error ? error.message : 'Erreur lors de la suppression du Quezap',
          ),
        }),
      ),
    )
  }
}

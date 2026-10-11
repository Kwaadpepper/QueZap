import { Injectable } from '@angular/core'

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
  QuestionId, QuestionType, QuestionWithAnswersAndResponses, Quezap, QuezapId,
  QuezapWithQuestionsAndAnswers, QuezapWithTheme, Theme, ThemeId,
} from '@quezap/domain/models'

import { QuezapService } from './quezap'

@Injectable({ providedIn: 'root' })
export class QuezapHttpService implements QuezapService {
  getQuezapPage(page: Pagination): ServiceOutput<PageOf<QuezapWithTheme>> {
    const p = toPageBasedPagination(page)

    return from(
      client.get<{ 200: PageOfDtoQuezapSummaryDto }>({
        url: '/apiv1/quezaps',
        query: {
          page: p.page,
          pageSize: p.pageSize,
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
            questionWithAnswersAndResponses: slides.map(s => ({
              id: (s.question?.id ?? '') as QuestionId,
              value: s.question?.label ?? '',
              type: (s.question?.type as QuestionType) || QuestionType.Quizz,
              limit: { seconds: s.timer || 30 },
              answers: (s.question?.answers || []).map((a, idx) => ({
                index: idx,
                points: s.points || 1000,
                value: a.label ?? '',
                isCorrect: a.isCorrect ?? false,
              })),
            })),
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
        answerOrder: aIdx + 1,
      }))

      const inlineQuestion: InlineQuestionRequest = {
        id: q.id && String(q.id).length > 0 ? q.id : undefined,
        label: q.value,
        type: q.type || 'QUIZZ',
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
}

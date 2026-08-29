import { HttpClient, HttpParams } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'

import { catchError, map, of } from 'rxjs'

import { NotFoundError, ValidationError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services/config/config'
import { PageOf, Pagination, ServiceOutput, toPageBasedPagination } from '@quezap/core/types'
import {
  QuestionId, QuestionType, Quezap, QuezapId,
  QuezapWithQuestionsAndAnswers, QuezapWithTheme, Theme, ThemeId,
} from '@quezap/domain/models'
import { UUID } from '@quezap/domain/types'

import { QuezapService } from './quezap'

interface QuezapApiResponse {
  id: string
  title: string
  description?: string
  authorId?: string
  isPublic?: boolean
  theme?: {
    id: string
    name: string
  }
  questions?: {
    id: string
    title: string
    type: string
    timerSeconds: number
    points: number
    answers: {
      id: string
      label: string
      isCorrect: boolean
    }[]
  }[]
}

interface PageOfApiResponse<T> {
  data: T[]
  totalElements: number
  totalPages: number
  page: number
  pageSize: number
  hasNext: boolean
  hasPrevious: boolean
}

@Injectable({ providedIn: 'root' })
export class QuezapHttpService implements QuezapService {
  private readonly http = inject(HttpClient)
  private readonly config = inject(Config)

  private get apiUrl(): string {
    return this.config.appConfig.value().apiUrl
  }

  getQuezapPage(page: Pagination): ServiceOutput<PageOf<QuezapWithTheme>> {
    const p = toPageBasedPagination(page)
    const params = new HttpParams()
      .set('page', p.page.toString())
      .set('pageSize', p.pageSize.toString())

    return this.http.get<PageOfApiResponse<QuezapApiResponse>>(`${this.apiUrl}/quezaps`, { params }).pipe(
      map(res => ({
        kind: 'success' as const,
        result: {
          data: res.data.map(q => ({
            id: q.id as QuezapId,
            title: q.title,
            description: q.description || '',
            theme: {
              id: (q.theme?.id || '018e3a2b-8888-7def-8123-abcdef888888') as ThemeId,
              name: q.theme?.name || 'Général',
            } as Theme,
          })),
          totalElements: res.totalElements,
          totalPages: res.totalPages,
          page: res.page,
          pageSize: res.pageSize,
          hasNext: res.hasNext,
          hasPrevious: res.hasPrevious,
        },
      })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new NotFoundError(error.message || 'Erreur lors du chargement des quezaps'),
      })),
    )
  }

  find(quezapId: QuezapId): ServiceOutput<QuezapWithQuestionsAndAnswers, NotFoundError> {
    return this.http.get<QuezapApiResponse>(`${this.apiUrl}/quezaps/${quezapId}`).pipe(
      map(res => ({
        kind: 'success' as const,
        result: {
          id: res.id as QuezapId,
          title: res.title,
          description: res.description || '',
          questionWithAnswersAndResponses: (res.questions || []).map(q => ({
            id: q.id as QuestionId,
            value: q.title,
            type: (q.type as QuestionType) || QuestionType.Quizz,
            limit: { seconds: q.timerSeconds },
            answers: (q.answers || []).map((a, idx) => ({
              index: idx,
              points: q.points || 1000,
              value: a.label,
              isCorrect: a.isCorrect,
            })),
          })),
        },
      })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new NotFoundError(error.message || 'Quezap introuvable'),
      })),
    )
  }

  persistQuezap(quezap: Omit<QuezapWithQuestionsAndAnswers, 'id'>): ServiceOutput<Pick<Quezap, 'id'>> {
    const payload = {
      title: quezap.title,
      description: quezap.description || '',
      isPublic: true,
      questions: quezap.questionWithAnswersAndResponses.map(q => ({
        questionId: q.id as UUID,
        timerSeconds: q.limit?.seconds || 30,
        points: q.answers[0]?.points || 1000,
      })),
    }

    return this.http.post<{ id: string }>(`${this.apiUrl}/quezaps`, payload).pipe(
      map(res => ({
        kind: 'success' as const,
        result: { id: res.id as QuezapId },
      })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new ValidationError({}, error.message || 'Erreur lors de la sauvegarde du Quezap'),
      })),
    )
  }
}

import { HttpClient, HttpParams } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'

import { catchError, map, of } from 'rxjs'

import { NotFoundError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services/config/config'
import { PageOf, Pagination, ServiceOutput, toPageBasedPagination } from '@quezap/core/types'
import { Session, SessionCode, SessionId } from '@quezap/domain/models'

import { SessionService, SessionApiResponse, ScoreResponse } from './session'

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
export class SessionHttpService implements SessionService {
  private readonly http = inject(HttpClient)
  private readonly config = inject(Config)

  private get apiUrl(): string {
    return this.config.appConfig.value().apiUrl
  }

  getSessionPage(page: Pagination): ServiceOutput<PageOf<Session>> {
    const p = toPageBasedPagination(page)
    const params = new HttpParams()
      .set('page', p.page.toString())
      .set('pageSize', p.pageSize.toString())

    return this.http.get<PageOfApiResponse<SessionApiResponse>>(`${this.apiUrl}/sessions`, { params }).pipe(
      map(res => ({
        kind: 'success' as const,
        result: {
          data: res.data.map(s => ({
            id: s.id as SessionId,
            name: s.name,
            code: (s.sessionCode || 'QZ1234') as SessionCode,
            startedAt: s.startedAt ? new Date(s.startedAt) : null,
            endedAt: s.endedAt ? new Date(s.endedAt) : null,
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
        error: new NotFoundError(error.message || 'Erreur lors du chargement des sessions'),
      })),
    )
  }

  getSession(id: string): ServiceOutput<SessionApiResponse> {
    return this.http.get<SessionApiResponse>(`${this.apiUrl}/sessions/${id}`).pipe(
      map(res => ({ kind: 'success' as const, result: res })),
      catchError(error => of({ kind: 'failure' as const, error: new NotFoundError(error.message) })),
    )
  }

  createSession(quezapId: string, name: string): ServiceOutput<SessionApiResponse> {
    return this.http.post<SessionApiResponse>(`${this.apiUrl}/sessions`, { quezapId, name }).pipe(
      map(res => ({ kind: 'success' as const, result: res })),
      catchError(error => of({ kind: 'failure' as const, error: new NotFoundError(error.message) })),
    )
  }

  startSession(id: string): ServiceOutput<void> {
    return this.http.post<void>(`${this.apiUrl}/sessions/${id}/start`, {}).pipe(
      map(() => ({ kind: 'success' as const, result: undefined })),
      catchError(error => of({ kind: 'failure' as const, error: new NotFoundError(error.message) })),
    )
  }

  nextQuestion(id: string): ServiceOutput<void> {
    return this.http.post<void>(`${this.apiUrl}/sessions/${id}/next-question`, {}).pipe(
      map(() => ({ kind: 'success' as const, result: undefined })),
      catchError(error => of({ kind: 'failure' as const, error: new NotFoundError(error.message) })),
    )
  }

  getScores(id: string): ServiceOutput<ScoreResponse[]> {
    return this.http.get<ScoreResponse[]>(`${this.apiUrl}/sessions/${id}/scores`).pipe(
      map(res => ({ kind: 'success' as const, result: res })),
      catchError(error => of({ kind: 'failure' as const, error: new NotFoundError(error.message) })),
    )
  }

  kickParticipant(sessionId: string, name: string): ServiceOutput<void> {
    return this.http.post<void>(`${this.apiUrl}/sessions/${sessionId}/participants/${name}/kick`, {}).pipe(
      map(() => ({ kind: 'success' as const, result: undefined })),
      catchError(error => of({ kind: 'failure' as const, error: new NotFoundError(error.message) })),
    )
  }
}

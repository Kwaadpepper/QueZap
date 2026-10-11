import { HttpClient, HttpParams } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'

import { catchError, map, of } from 'rxjs'

import { LeaderboardResponseDto, SessionReportDto } from '@quezap/core/api-client'
import { NotFoundError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services/config/config'
import { PageOf, Pagination, ServiceOutput, toPageBasedPagination } from '@quezap/core/types'
import { Session, SessionCode, SessionId } from '@quezap/domain/models'

import { SessionApiResponse, SessionService } from './session'

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
            code: (s.code || s.sessionCode || 'QZ1234') as SessionCode,
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
    const isUuid = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(id)
    const endpoint = isUuid
      ? `${this.apiUrl}/sessions/${id}`
      : `${this.apiUrl}/sessions/by-code/${id.replace(/\s+/g, '')}`
    return this.http.get<SessionApiResponse>(endpoint).pipe(
      map(res => ({ kind: 'success' as const, result: res })),
      catchError(error => of({ kind: 'failure' as const, error: new NotFoundError(error.message) })),
    )
  }

  createSession(quezapId: string, name: string): ServiceOutput<SessionApiResponse> {
    return this.http.post<SessionApiResponse>(`${this.apiUrl}/sessions`, { quezapId, name }).pipe(
      map(res => ({ kind: 'success' as const, result: res })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new NotFoundError(error.error?.message || error.error?.detail || error.message || 'Erreur de création'),
      })),
    )
  }

  startSession(id: string): ServiceOutput<void> {
    return this.http.post<void>(`${this.apiUrl}/sessions/${id}/start`, {}).pipe(
      map(() => ({ kind: 'success' as const, result: undefined })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new NotFoundError(
          error.error?.message || error.error?.detail || error.message || 'Erreur de démarrage',
        ),
      })),
    )
  }

  nextQuestion(id: string): ServiceOutput<void> {
    return this.http.post<void>(`${this.apiUrl}/sessions/${id}/next-question`, {}).pipe(
      map(() => ({ kind: 'success' as const, result: undefined })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new NotFoundError(
          error.error?.message || error.error?.detail || error.message || 'Erreur question suivante',
        ),
      })),
    )
  }

  revealAnswers(id: string): ServiceOutput<void> {
    return this.http.post<void>(`${this.apiUrl}/sessions/${id}/reveal`, {}).pipe(
      map(() => ({ kind: 'success' as const, result: undefined })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new NotFoundError(
          error.error?.message || error.error?.detail || error.message || 'Erreur révélation réponses',
        ),
      })),
    )
  }

  getScores(id: string): ServiceOutput<LeaderboardResponseDto> {
    return this.http.get<LeaderboardResponseDto>(`${this.apiUrl}/sessions/${id}/scores`).pipe(
      map(res => ({ kind: 'success' as const, result: res })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new NotFoundError(
          error.error?.message || error.error?.detail || error.message || 'Erreur récupération scores',
        ),
      })),
    )
  }

  getSessionReport(id: string): ServiceOutput<SessionReportDto> {
    return this.http.get<SessionReportDto>(`${this.apiUrl}/sessions/${id}/report`).pipe(
      map(res => ({ kind: 'success' as const, result: res })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new NotFoundError(
          error.error?.message || error.error?.detail || error.message || 'Erreur récupération rapport',
        ),
      })),
    )
  }

  kickParticipant(sessionId: string, name: string): ServiceOutput<void> {
    return this.http.post<void>(`${this.apiUrl}/sessions/${sessionId}/participants/${name}/kick`, {}).pipe(
      map(() => ({ kind: 'success' as const, result: undefined })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new NotFoundError(
          error.error?.message || error.error?.detail || error.message || 'Erreur exclusion joueur',
        ),
      })),
    )
  }
}

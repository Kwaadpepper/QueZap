import { Injectable } from '@angular/core'

import { catchError, from, map, of } from 'rxjs'

import {
  create1,
  ExportCsvData,
  exportCsv as sdkExportCsv,
  getByCode,
  getById2,
  getReport,
  getScores as sdkGetScores,
  kickParticipant as sdkKickParticipant,
  LeaderboardResponseDto,
  list1,
  nextQuestion as sdkNextQuestion,
  type Options,
  pauseSession as sdkPauseSession,
  reveal,
  SessionCreatedDto,
  SessionDetailDto,
  SessionPauseResponseDto,
  SessionReportDto,
  SessionSettingsRequestDto,
  SessionSettingsResponseDto,
  start,
  updateSettings as sdkUpdateSettings,
} from '@quezap/core/api-client'
import { NotFoundError } from '@quezap/core/errors'
import { PageOf, Pagination, ServiceOutput, toPageBasedPagination } from '@quezap/core/types'
import { Session, SessionCode, SessionId } from '@quezap/domain/models'

import { SessionService } from './session'

@Injectable({ providedIn: 'root' })
export class SessionHttpService implements SessionService {
  getSessionPage(page: Pagination): ServiceOutput<PageOf<Session>> {
    const p = toPageBasedPagination(page)
    const perPage = Math.min(Math.max(p.pageSize, 1), 50)

    return from(
      list1({
        query: {
          paginationDto: {
            page: p.page,
            perPage,
          },
        },
        querySerializer: {
          object: {
            style: 'form',
            explode: true,
          },
        },
      }),
    ).pipe(
      map(({ data, error }) => {
        if (error || !data) {
          return {
            kind: 'failure' as const,
            error: new NotFoundError('Erreur lors du chargement des sessions'),
          }
        }

        return {
          kind: 'success' as const,
          result: {
            data: (data.data ?? []).map(s => ({
              id: (s.id ?? '') as SessionId,
              name: s.name ?? '',
              code: (s.code ?? 'QZ1234') as SessionCode,
              status: (s.status as 'LOBBY' | 'STARTED' | 'ENDED') || undefined,
              startedAt: s.startedAt ? new Date(s.startedAt) : null,
              endedAt: s.endedAt ? new Date(s.endedAt) : null,
            })),
            totalElements: data.totalElements ?? 0,
            totalPages: data.totalPages ?? 0,
            page: data.page ?? p.page,
            pageSize: data.pageSize ?? perPage,
            hasNext: data.hasNext ?? false,
            hasPrevious: data.hasPrevious ?? false,
          },
        }
      }),
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new NotFoundError('Erreur lors du chargement des sessions'),
        }),
      ),
    )
  }

  getSession(id: string): ServiceOutput<SessionDetailDto> {
    const isUuid = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(id)
    const request = isUuid
      ? getById2({ path: { id } })
      : getByCode({ path: { code: id.replace(/\s+/g, '') } })

    return from(request).pipe(
      map(({ data, error }) => {
        if (error || !data) {
          return {
            kind: 'failure' as const,
            error: new NotFoundError('Session introuvable'),
          }
        }
        return {
          kind: 'success' as const,
          result: data,
        }
      }),
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new NotFoundError('Session introuvable'),
        }),
      ),
    )
  }

  createSession(quezapId: string, name: string): ServiceOutput<SessionCreatedDto> {
    return from(
      create1({ body: { quezapId, name } }),
    ).pipe(
      map(({ data, error }) => {
        if (error || !data) {
          return {
            kind: 'failure' as const,
            error: new NotFoundError('Erreur de création de session'),
          }
        }
        return {
          kind: 'success' as const,
          result: data,
        }
      }),
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new NotFoundError('Erreur de création de session'),
        }),
      ),
    )
  }

  startSession(id: string): ServiceOutput<void> {
    return from(
      start({ path: { id } }),
    ).pipe(
      map(({ error }) => {
        if (error) {
          return {
            kind: 'failure' as const,
            error: new NotFoundError('Erreur de démarrage'),
          }
        }
        return { kind: 'success' as const, result: undefined }
      }),
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new NotFoundError('Erreur de démarrage'),
        }),
      ),
    )
  }

  nextQuestion(id: string): ServiceOutput<void> {
    return from(
      sdkNextQuestion({ path: { id } }),
    ).pipe(
      map(({ error }) => {
        if (error) {
          return {
            kind: 'failure' as const,
            error: new NotFoundError('Erreur question suivante'),
          }
        }
        return { kind: 'success' as const, result: undefined }
      }),
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new NotFoundError('Erreur question suivante'),
        }),
      ),
    )
  }

  revealAnswers(id: string): ServiceOutput<void> {
    return from(
      reveal({ path: { id } }),
    ).pipe(
      map(({ error }) => {
        if (error) {
          return {
            kind: 'failure' as const,
            error: new NotFoundError('Erreur révélation réponses'),
          }
        }
        return { kind: 'success' as const, result: undefined }
      }),
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new NotFoundError('Erreur révélation réponses'),
        }),
      ),
    )
  }

  getScores(id: string): ServiceOutput<LeaderboardResponseDto> {
    return from(
      sdkGetScores({ path: { id } }),
    ).pipe(
      map(({ data, error }) => {
        if (error || !data) {
          return {
            kind: 'failure' as const,
            error: new NotFoundError('Erreur récupération scores'),
          }
        }
        return {
          kind: 'success' as const,
          result: data,
        }
      }),
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new NotFoundError('Erreur récupération scores'),
        }),
      ),
    )
  }

  getSessionReport(id: string): ServiceOutput<SessionReportDto> {
    return from(
      getReport({ path: { id } }),
    ).pipe(
      map(({ data, error }) => {
        if (error || !data) {
          return {
            kind: 'failure' as const,
            error: new NotFoundError('Erreur récupération rapport'),
          }
        }
        return {
          kind: 'success' as const,
          result: data,
        }
      }),
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new NotFoundError('Erreur récupération rapport'),
        }),
      ),
    )
  }

  exportCsv(id: string): ServiceOutput<Blob> {
    return from(
      sdkExportCsv({
        path: { id },
        responseType: 'blob',
      } as Options<ExportCsvData, false> & { responseType: 'blob' }),
    ).pipe(
      map((res) => {
        const error = 'error' in res ? res.error : undefined
        const data = 'data' in res ? res.data : undefined
        if (error || !data) {
          return {
            kind: 'failure' as const,
            error: new NotFoundError('Échec du téléchargement du fichier CSV'),
          }
        }
        const blob = typeof data === 'string'
          ? new Blob([data], { type: 'text/csv;charset=utf-8;' })
          : data
        return {
          kind: 'success' as const,
          result: blob,
        }
      }),
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new NotFoundError('Échec du téléchargement du fichier CSV'),
        }),
      ),
    )
  }

  kickParticipant(sessionId: string, name: string): ServiceOutput<void> {
    return from(
      sdkKickParticipant({ path: { id: sessionId, name } }),
    ).pipe(
      map(({ error }) => {
        if (error) {
          return {
            kind: 'failure' as const,
            error: new NotFoundError('Erreur exclusion joueur'),
          }
        }
        return { kind: 'success' as const, result: undefined }
      }),
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new NotFoundError('Erreur exclusion joueur'),
        }),
      ),
    )
  }

  pauseSession(id: string, paused: boolean): ServiceOutput<SessionPauseResponseDto> {
    return from(
      sdkPauseSession({
        path: { id },
        body: { paused },
      }),
    ).pipe(
      map(({ data, error }) => {
        if (error || !data) {
          return {
            kind: 'failure' as const,
            error: new NotFoundError('Erreur mise en pause de la session'),
          }
        }
        return { kind: 'success' as const, result: data }
      }),
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new NotFoundError('Erreur mise en pause de la session'),
        }),
      ),
    )
  }

  updateSettings(id: string, settings: SessionSettingsRequestDto): ServiceOutput<SessionSettingsResponseDto> {
    return from(
      sdkUpdateSettings({
        path: { id },
        body: settings,
      }),
    ).pipe(
      map(({ data, error }) => {
        if (error || !data) {
          return {
            kind: 'failure' as const,
            error: new NotFoundError('Erreur mise à jour des réglages'),
          }
        }
        return { kind: 'success' as const, result: data }
      }),
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new NotFoundError('Erreur mise à jour des réglages'),
        }),
      ),
    )
  }
}

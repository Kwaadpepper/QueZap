import { Injectable } from '@angular/core'

import { from, map } from 'rxjs'

import {
  LeaderboardResponseDto,
  PageOfDtoSessionDetailDto,
  SessionDetailDto,
  SessionReportDto,
} from '@quezap/core/api-client'
import { client } from '@quezap/core/api-client/client.gen'
import { NotFoundError } from '@quezap/core/errors'
import { PageOf, Pagination, ServiceOutput, toPageBasedPagination } from '@quezap/core/types'
import { Session, SessionCode, SessionId } from '@quezap/domain/models'

import { SessionService } from './session'

@Injectable({ providedIn: 'root' })
export class SessionHttpService implements SessionService {
  getSessionPage(page: Pagination): ServiceOutput<PageOf<Session>> {
    const p = toPageBasedPagination(page)

    return from(
      client.get<{ 200: PageOfDtoSessionDetailDto }>({
        url: '/apiv1/sessions',
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
            pageSize: data.pageSize ?? p.pageSize,
            hasNext: data.hasNext ?? false,
            hasPrevious: data.hasPrevious ?? false,
          },
        }
      }),
    )
  }

  getSession(id: string): ServiceOutput<SessionDetailDto> {
    const isUuid = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(id)
    const url = isUuid ? '/apiv1/sessions/{id}' : '/apiv1/sessions/by-code/{code}'
    const path = isUuid ? { id } : { code: id.replace(/\s+/g, '') }

    return from(
      client.get<{ 200: SessionDetailDto }>({
        url,
        path,
      }),
    ).pipe(
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
    )
  }

  createSession(quezapId: string, name: string): ServiceOutput<SessionDetailDto> {
    return from(
      client.post<{ 200: SessionDetailDto, 201: SessionDetailDto }>({
        url: '/apiv1/sessions',
        body: { quezapId, name },
      }),
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
    )
  }

  startSession(id: string): ServiceOutput<void> {
    return from(
      client.post<{ 200: void, 204: void }>({
        url: '/apiv1/sessions/{id}/start',
        path: { id },
      }),
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
    )
  }

  nextQuestion(id: string): ServiceOutput<void> {
    return from(
      client.post<{ 200: void, 204: void }>({
        url: '/apiv1/sessions/{id}/next-question',
        path: { id },
      }),
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
    )
  }

  revealAnswers(id: string): ServiceOutput<void> {
    return from(
      client.post<{ 200: void, 204: void }>({
        url: '/apiv1/sessions/{id}/reveal',
        path: { id },
      }),
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
    )
  }

  getScores(id: string): ServiceOutput<LeaderboardResponseDto> {
    return from(
      client.get<{ 200: LeaderboardResponseDto }>({
        url: '/apiv1/sessions/{id}/scores',
        path: { id },
      }),
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
    )
  }

  getSessionReport(id: string): ServiceOutput<SessionReportDto> {
    return from(
      client.get<{ 200: SessionReportDto }>({
        url: '/apiv1/sessions/{id}/report',
        path: { id },
      }),
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
    )
  }

  kickParticipant(sessionId: string, name: string): ServiceOutput<void> {
    return from(
      client.post<{ 200: void, 204: void }>({
        url: '/apiv1/sessions/{id}/participants/{name}/kick',
        path: { id: sessionId, name },
      }),
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
    )
  }
}

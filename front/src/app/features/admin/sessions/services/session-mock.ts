import { Injectable, signal } from '@angular/core'

import { delay, map, Observable, of, tap } from 'rxjs'

import { ServiceError } from '@quezap/core/errors'
import { PageOf, Pagination, ServiceOutput, toPageBasedPagination } from '@quezap/core/types'
import { Session } from '@quezap/domain/models'

import { SessionService, SessionApiResponse, ScoreResponse } from './session'
import { MOCK_SESSIONS } from './session.mock'

@Injectable()
export class SessionMockService implements SessionService {
  private readonly MOCK_DELAY = () => Math.max(100, Math.random() * 3000)
  private readonly MOCK_ERROR = () => Math.random() < 0.2

  private readonly mockedSessions = signal(MOCK_SESSIONS)

  getSessionPage(page: Pagination): ServiceOutput<PageOf<Session>> {
    const pagination = toPageBasedPagination(page)
    const sessions: Session[] = this.mockedSessions().map(session => ({
      id: session.id,
      name: session.name,
      code: session.code,
      startedAt: session.startedAt,
      endedAt: session.endedAt,
    }))

    const startIndex = (pagination.page - 1) * pagination.pageSize
    const endIndex = startIndex + pagination.pageSize
    const pagedSessions = sessions.slice(startIndex, endIndex)

    const pageOfSessions: PageOf<Session> = {
      data: pagedSessions,
      totalElements: sessions.length,
      totalPages: Math.ceil(sessions.length / pagination.pageSize),
      page: pagination.page,
      pageSize: pagination.pageSize,
      hasNext: endIndex < sessions.length,
      hasPrevious: startIndex > 0,
    }

    return of(pageOfSessions).pipe(
      delay(this.MOCK_DELAY()),
      tap(() => {
        if (this.MOCK_ERROR()) {
          console.debug('Mock: error while fetching sessions')
          throw new ServiceError('Mock service error: sessions')
        }
      }),
      map((obs) => {
        console.debug('Mock: fetched sessions page', pageOfSessions)
        return {
          kind: 'success',
          result: obs,
        }
      }),
    )
  }

  getSession(_id: string): Observable<SessionApiResponse> {
    return of({
      id: _id,
      sessionNumber: 1,
      sessionCode: 'QZ-8888',
      name: 'Mock Session',
      status: 'LOBBY',
      currentSlideIndex: 0,
      createdAt: new Date().toISOString(),
    })
  }

  createSession(_quezapId: string, _name: string): Observable<SessionApiResponse> {
    return of({
      id: 'mock-id',
      sessionNumber: 1,
      name: _name,
      status: 'LOBBY',
      currentSlideIndex: 0,
      createdAt: new Date().toISOString(),
    })
  }

  startSession(_id: string): Observable<void> { return of(void 0) }
  nextQuestion(_id: string): Observable<void> { return of(void 0) }
  getScores(_id: string): Observable<ScoreResponse[]> { return of([]) }
  kickParticipant(_sessionId: string, _name: string): Observable<void> { return of(void 0) }
}

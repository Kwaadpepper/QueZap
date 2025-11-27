import { Injectable, signal } from '@angular/core'

import { delay, map, of, tap } from 'rxjs'

import { ServiceError } from '@quezap/core/errors'
import { PageOf, Pagination, ServiceOutput, toPageBasedPagination } from '@quezap/core/types'
import { Session } from '@quezap/domain/models'

import { SessionService } from './session'
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
}

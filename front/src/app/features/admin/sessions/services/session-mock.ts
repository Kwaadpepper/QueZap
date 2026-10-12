import { Injectable, signal } from '@angular/core'

import { delay, map, of, tap } from 'rxjs'

import {
  LeaderboardResponseDto,
  SessionDetailDto,
  SessionPauseResponseDto,
  SessionReportDto,
  SessionSettingsRequestDto,
  SessionSettingsResponseDto,
} from '@quezap/core/api-client'
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

  getSession(_id: string): ServiceOutput<SessionDetailDto> {
    return of({
      kind: 'success' as const,
      result: {
        id: _id,
        number: 1,
        code: 'QZ-8888',
        name: 'Mock Session',
        status: 'LOBBY',
        currentSlideIndex: 0,
      },
    })
  }

  createSession(_quezapId: string, _name: string): ServiceOutput<SessionDetailDto> {
    return of({
      kind: 'success' as const,
      result: {
        id: 'mock-id',
        number: 1,
        name: _name,
        status: 'LOBBY',
        currentSlideIndex: 0,
      },
    })
  }

  startSession(_id: string): ServiceOutput<void> {
    return of({ kind: 'success' as const, result: undefined })
  }

  nextQuestion(_id: string): ServiceOutput<void> {
    return of({ kind: 'success' as const, result: undefined })
  }

  revealAnswers(_id: string): ServiceOutput<void> {
    return of({ kind: 'success' as const, result: undefined })
  }

  getScores(_id: string): ServiceOutput<LeaderboardResponseDto> {
    return of({
      kind: 'success' as const,
      result: {
        sessionId: _id,
        slideIndex: 1,
        leaderboard: [],
      },
    })
  }

  getSessionReport(_id: string): ServiceOutput<SessionReportDto> {
    return of({
      kind: 'success' as const,
      result: {
        sessionId: _id,
        sessionNumber: 1042,
        sessionCode: 'QZ-1042',
        sessionName: 'Mock Session',
        startedAt: new Date().toISOString(),
        endedAt: new Date().toISOString(),
        totalParticipants: 0,
        totalQuestions: 0,
        averageScore: 0,
        averageAccuracyRate: 0,
        participants: [],
        questions: [],
      },
    })
  }

  kickParticipant(_sessionId: string, _name: string): ServiceOutput<void> {
    return of({ kind: 'success' as const, result: undefined })
  }

  pauseSession(id: string, paused: boolean): ServiceOutput<SessionPauseResponseDto> {
    return of({
      kind: 'success' as const,
      result: {
        sessionId: id,
        isPaused: paused,
      },
    })
  }

  updateSettings(_id: string, settings: SessionSettingsRequestDto): ServiceOutput<SessionSettingsResponseDto> {
    return of({
      kind: 'success' as const,
      result: {
        progressionMode: settings.progressionMode ?? 'MANUAL',
        explanationDurationSeconds: settings.explanationDurationSeconds ?? 15,
        mobileCorrectionEnabled: settings.mobileCorrectionEnabled ?? false,
      },
    })
  }
}

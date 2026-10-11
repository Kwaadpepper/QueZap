import { InjectionToken } from '@angular/core'

import { LeaderboardResponseDto } from '@quezap/core/api-client'
import { PageOf, Pagination, ServiceOutput } from '@quezap/core/types'
import { Session } from '@quezap/domain/models'

export interface SessionApiResponse {
  id: string
  sessionNumber: number
  sessionCode?: string
  code?: string
  name: string
  status: 'LOBBY' | 'STARTED' | 'ENDED'
  currentSlideIndex: number
  participantsCount?: number
  startedAt?: string
  endedAt?: string
  createdAt: string
}

export interface ScoreResponse {
  name: string
  score: number
  rank: number
}

export interface SessionService {
  getSessionPage(page: Pagination): ServiceOutput<PageOf<Session>>
  getSession(id: string): ServiceOutput<SessionApiResponse>
  createSession(quezapId: string, name: string): ServiceOutput<SessionApiResponse>
  startSession(id: string): ServiceOutput<void>
  nextQuestion(id: string): ServiceOutput<void>
  getScores(id: string): ServiceOutput<LeaderboardResponseDto>
  kickParticipant(sessionId: string, name: string): ServiceOutput<void>
}

export const SESSION_SERVICE = new InjectionToken<SessionService>('SessionService')

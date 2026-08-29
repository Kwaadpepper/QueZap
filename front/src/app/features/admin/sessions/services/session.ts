import { InjectionToken } from '@angular/core'

import { Observable } from 'rxjs'

import { PageOf, Pagination, ServiceOutput } from '@quezap/core/types'
import { Session } from '@quezap/domain/models'

export interface SessionApiResponse {
  id: string
  sessionNumber: number
  sessionCode?: string
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
  getSession(id: string): Observable<SessionApiResponse>
  createSession(quezapId: string, name: string): Observable<SessionApiResponse>
  startSession(id: string): Observable<void>
  nextQuestion(id: string): Observable<void>
  getScores(id: string): Observable<ScoreResponse[]>
  kickParticipant(sessionId: string, name: string): Observable<void>
}

export const SESSION_SERVICE = new InjectionToken<SessionService>('SessionService')

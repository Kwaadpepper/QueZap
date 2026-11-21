import { InjectionToken } from '@angular/core'

import {
  LeaderboardResponseDto,
  SessionCreatedDto,
  SessionDetailDto,
  SessionPauseResponseDto,
  SessionReportDto,
  SessionSettingsRequestDto,
  SessionSettingsResponseDto,
  SseTicketResponseDto,
} from '@quezap/core/api-client'
import { PageOf, Pagination, ServiceOutput } from '@quezap/core/types'
import { Session } from '@quezap/domain/models'

export interface ScoreResponse {
  name: string
  score: number
  rank: number
}

export interface SessionService {
  getSessionPage(page: Pagination): ServiceOutput<PageOf<Session>>
  getSession(id: string): ServiceOutput<SessionDetailDto>
  createSession(quezapId: string, name: string): ServiceOutput<SessionCreatedDto>
  createEventsTicket(id: string): ServiceOutput<SseTicketResponseDto>
  startSession(id: string): ServiceOutput<void>
  nextQuestion(id: string): ServiceOutput<void>
  revealAnswers(id: string): ServiceOutput<void>
  getScores(id: string): ServiceOutput<LeaderboardResponseDto>
  getSessionReport(id: string): ServiceOutput<SessionReportDto>
  exportCsv(id: string): ServiceOutput<Blob>
  kickParticipant(sessionId: string, name: string): ServiceOutput<void>
  pauseSession(id: string, paused: boolean): ServiceOutput<SessionPauseResponseDto>
  updateSettings(id: string, settings: SessionSettingsRequestDto): ServiceOutput<SessionSettingsResponseDto>
}

export const SESSION_SERVICE = new InjectionToken<SessionService>('SessionService')

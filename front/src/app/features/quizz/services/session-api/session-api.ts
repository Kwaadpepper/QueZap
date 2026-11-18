import { InjectionToken } from '@angular/core'

import { LeaderboardResponseDto } from '@quezap/core/api-client'
import { ExpiredError, ForbidenError, NotFoundError, ValidationError } from '@quezap/core/errors'
import { ServiceOutput } from '@quezap/core/types'
import { Session, SessionCode } from '@quezap/domain/models'

export interface SessionApiService {
  joinSessionWith(code: SessionCode): ServiceOutput<Session, NotFoundError | ExpiredError>

  chooseNickname(nickname: string): ServiceOutput<void, ForbidenError | ValidationError>

  submitAnswer(answerIndex?: number, textAnswer?: string): ServiceOutput<void>
  leaveSession?(): ServiceOutput<void>
  getSessionScores?(sessionId: string): ServiceOutput<LeaderboardResponseDto>
}

export const SESSION_API_SERVICE = new InjectionToken<SessionApiService>('SessionApiService')

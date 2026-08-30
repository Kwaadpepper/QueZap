import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'

import { catchError, map, of } from 'rxjs'

import { ExpiredError, ForbidenError, NotFoundError, ValidationError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services/config/config'
import { ServiceOutput } from '@quezap/core/types'
import { Session, SessionCode, SessionId } from '@quezap/domain/models'

import { ActiveSessionPersistence } from '../active-session-persistence/active-session-persistence'

import { SessionApiService } from './session-api'

interface SessionByCodeResponse {
  id: string
  number: number
  code: string
  name: string
  status: 'LOBBY' | 'STARTED' | 'ENDED'
  currentSlideIndex: number
  totalSlides: number
  participantsCount: number
  authorId: string
}

interface JoinSessionApiResponse {
  sessionId: string
  sessionNumber?: number
  sessionCode?: string
  name?: string
  status?: 'LOBBY' | 'STARTED' | 'ENDED'
  participationToken: string
}

@Injectable({ providedIn: 'root' })
export class SessionApiHttpService implements SessionApiService {
  private readonly http = inject(HttpClient)
  private readonly config = inject(Config)
  private readonly sessionPersistence = inject(ActiveSessionPersistence)

  private currentSessionCode = ''
  private currentParticipationToken = ''
  private currentSessionId = ''

  private get apiUrl(): string {
    return this.config.appConfig.value().apiUrl
  }

  joinSessionWith(code: SessionCode): ServiceOutput<Session, NotFoundError | ExpiredError> {
    this.currentSessionCode = typeof code === 'string' ? code : String(code)

    return this.http.get<SessionByCodeResponse>(`${this.apiUrl}/sessions/by-code/${this.currentSessionCode}`).pipe(
      map((res) => {
        this.currentSessionId = res.id
        this.sessionPersistence.setSessionId(res.id)
        this.sessionPersistence.patch({
          code: (res.code || this.currentSessionCode) as SessionCode,
          sessionId: res.id,
        })
        const session: Session = {
          id: res.id as SessionId,
          code: (res.code || this.currentSessionCode) as SessionCode,
          name: res.name,
          startedAt: res.status === 'STARTED' ? new Date() : null,
          endedAt: res.status === 'ENDED' ? new Date() : null,
        }
        return {
          kind: 'success' as const,
          result: session,
        }
      }),
      catchError((err: HttpErrorResponse) => {
        if (err.status === 404) {
          return of({
            kind: 'failure' as const,
            error: new NotFoundError('Session introuvable'),
          })
        }
        return of({
          kind: 'failure' as const,
          error: new ExpiredError('Session expirée ou erreur serveur'),
        })
      }),
    )
  }

  chooseNickname(nickname: string): ServiceOutput<void, ForbidenError | ValidationError> {
    const payload = {
      code: this.currentSessionCode,
      participantName: nickname,
    }

    return this.http.post<JoinSessionApiResponse>(`${this.apiUrl}/sessions/join`, payload).pipe(
      map((res) => {
        this.currentParticipationToken = res.participationToken
        this.currentSessionId = res.sessionId
        this.sessionPersistence.setSessionId(res.sessionId)
        this.sessionPersistence.setParticipationToken(res.participationToken)
        this.sessionPersistence.patch({
          sessionId: res.sessionId,
          participationToken: res.participationToken,
          nickname,
        })
        this.sessionPersistence.saveParticipationData(res.sessionId, res.participationToken, nickname)
        return {
          kind: 'success' as const,
          result: undefined,
        }
      }),
      catchError((err: HttpErrorResponse) => {
        if (err.status === 409 || err.status === 400) {
          return of({
            kind: 'failure' as const,
            error: new ValidationError({ nickname: [err.error?.message || 'Pseudo déjà utilisé ou invalide'] }),
          })
        }
        return of({
          kind: 'failure' as const,
          error: new ForbidenError(err.message || 'Impossible de rejoindre la session'),
        })
      }),
    )
  }

  submitAnswer(answerIndex: number): ServiceOutput<void> {
    const token = this.currentParticipationToken || this.sessionPersistence.getParticipationToken() || ''
    const sessionId = this.currentSessionId || this.sessionPersistence.getSessionId() || ''

    return this.http.post<void>(
      `${this.apiUrl}/sessions/${sessionId}/answers`,
      { answerIndex },
      { headers: { 'X-Participant-Token': token } },
    ).pipe(
      map(() => ({ kind: 'success' as const, result: undefined })),
      catchError(err => of({
        kind: 'failure' as const,
        error: new ValidationError({}, err.message || 'Erreur d\'envoi de la réponse'),
      })),
    )
  }
}

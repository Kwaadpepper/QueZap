import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'

import { catchError, map, of } from 'rxjs'

import { ExpiredError, ForbidenError, NotFoundError, ValidationError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services/config/config'
import { ServiceOutput } from '@quezap/core/types'
import { Session, SessionCode, SessionId } from '@quezap/domain/models'

import { SessionApiService } from './session-api'

interface JoinSessionApiResponse {
  sessionId: string
  sessionNumber: number
  sessionCode: string
  name: string
  status: 'LOBBY' | 'STARTED' | 'ENDED'
  participationToken: string
}

@Injectable({ providedIn: 'root' })
export class SessionApiHttpService implements SessionApiService {
  private readonly http = inject(HttpClient)
  private readonly config = inject(Config)

  private currentSessionCode = ''
  private currentParticipationToken = ''
  private currentSessionId = ''

  private get apiUrl(): string {
    return this.config.appConfig.value().apiUrl
  }

  joinSessionWith(code: SessionCode): ServiceOutput<Session, NotFoundError | ExpiredError> {
    this.currentSessionCode = typeof code === 'string' ? code : String(code)
    return of({
      id: '018e3a2b-8888-7def-8123-abcdef888888' as SessionId,
      name: `Session ${this.currentSessionCode}`,
      code: this.currentSessionCode as SessionCode,
      startedAt: null,
      endedAt: null,
    }).pipe(
      map(session => ({
        kind: 'success' as const,
        result: session,
      })),
    )
  }

  chooseNickname(nickname: string): ServiceOutput<void, ForbidenError | ValidationError> {
    const payload = {
      sessionCode: this.currentSessionCode,
      participantName: nickname,
    }

    return this.http.post<JoinSessionApiResponse>(`${this.apiUrl}/sessions/join`, payload).pipe(
      map((res) => {
        this.currentParticipationToken = res.participationToken
        this.currentSessionId = res.sessionId
        if (typeof sessionStorage !== 'undefined') {
          sessionStorage.setItem('quezap_participation_token', res.participationToken)
          sessionStorage.setItem('quezap_session_id', res.sessionId)
          sessionStorage.setItem('quezap_participant_name', nickname)
        }
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
    const storageToken = typeof sessionStorage !== 'undefined'
      ? sessionStorage.getItem('quezap_participation_token') || ''
      : ''
    const token = this.currentParticipationToken || storageToken

    const storageSessionId = typeof sessionStorage !== 'undefined'
      ? sessionStorage.getItem('quezap_session_id') || ''
      : ''
    const sessionId = this.currentSessionId || storageSessionId

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

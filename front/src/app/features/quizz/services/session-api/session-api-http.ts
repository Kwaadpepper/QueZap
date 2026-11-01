import { inject, Injectable } from '@angular/core'

import { from, map, of } from 'rxjs'

import { JoinSessionResponseDto, SessionDetailDto } from '@quezap/core/api-client'
import { client } from '@quezap/core/api-client/client.gen'
import { ExpiredError, ForbidenError, NotFoundError, ValidationError } from '@quezap/core/errors'
import { ServiceOutput } from '@quezap/core/types'
import { Session, SessionCode, SessionId } from '@quezap/domain/models'

import { ActiveSessionPersistence } from '../active-session-persistence/active-session-persistence'

import { SessionApiService } from './session-api'

@Injectable({ providedIn: 'root' })
export class SessionApiHttpService implements SessionApiService {
  private readonly sessionPersistence = inject(ActiveSessionPersistence)

  private currentSessionCode = ''
  private currentParticipationToken = ''
  private currentSessionId = ''

  joinSessionWith(code: SessionCode): ServiceOutput<Session, NotFoundError | ExpiredError> {
    this.currentSessionCode = (typeof code === 'string' ? code : String(code)).trim().toUpperCase().replace(/\s+/g, '')

    return from(
      client.get<{ 200: SessionDetailDto }, Record<string, unknown>>({
        url: '/apiv1/sessions/by-code/{code}',
        path: { code: this.currentSessionCode },
      }),
    ).pipe(
      map(({ data, error, response }) => {
        if (error || !data) {
          if (response?.status === 404) {
            return {
              kind: 'failure' as const,
              error: new NotFoundError('Session introuvable'),
            }
          }
          return {
            kind: 'failure' as const,
            error: new ExpiredError('Session expirée ou erreur serveur'),
          }
        }

        const id = data.id ?? ''
        this.currentSessionId = id
        this.sessionPersistence.setSessionId(id)
        this.sessionPersistence.patch({
          code: (data.code || this.currentSessionCode) as SessionCode,
          sessionId: id,
        })
        const session: Session = {
          id: id as SessionId,
          code: (data.code || this.currentSessionCode) as SessionCode,
          name: data.name ?? '',
          startedAt: data.status === 'STARTED' ? new Date() : null,
          endedAt: data.status === 'ENDED' ? new Date() : null,
        }
        return {
          kind: 'success' as const,
          result: session,
        }
      }),
    )
  }

  chooseNickname(nickname: string): ServiceOutput<void, ForbidenError | ValidationError> {
    const rawCode = this.currentSessionCode || this.sessionPersistence.retrieve()?.code || ''
    const code = rawCode.trim().toUpperCase()
    const payload = {
      code,
      participantName: nickname.trim(),
    }

    const existingToken = this.currentParticipationToken || this.sessionPersistence.getParticipationToken()
    const headers: Record<string, string> = existingToken ? { 'X-Participant-Token': existingToken } : {}

    return from(
      client.post<{ 200: JoinSessionResponseDto, 201: JoinSessionResponseDto }, Record<string, unknown>>({
        url: '/apiv1/sessions/join',
        body: payload,
        headers,
      }),
    ).pipe(
      map(({ data, error, response }) => {
        if (error || !data) {
          const status = response?.status
          if (status === 409 || status === 400) {
            const errObj = typeof error === 'object' && error !== null ? (error as { message?: string }) : undefined
            const detail
              = status === 409
                ? 'Ce pseudo est déjà utilisé dans ce salon. Choisis un autre prénom ou ajoute une initiale.'
                : errObj?.message || 'Pseudo déjà utilisé ou invalide'
            return {
              kind: 'failure' as const,
              error: new ValidationError({
                nickname: [detail],
                nicknameValue: [detail],
              }),
            }
          }
          return {
            kind: 'failure' as const,
            error: new ForbidenError('Impossible de rejoindre la session'),
          }
        }

        const token = data.participationToken ?? ''
        const sessId = data.sessionId ?? ''
        this.currentParticipationToken = token
        this.currentSessionId = sessId
        this.sessionPersistence.setSessionId(sessId)
        this.sessionPersistence.setParticipationToken(token)
        this.sessionPersistence.patch({
          sessionId: sessId,
          participationToken: token,
          nickname,
        })
        this.sessionPersistence.saveParticipationData(sessId, token, nickname)
        return {
          kind: 'success' as const,
          result: undefined,
        }
      }),
    )
  }

  submitAnswer(answerIndex?: number, textAnswer?: string): ServiceOutput<void> {
    const token = this.currentParticipationToken || this.sessionPersistence.getParticipationToken() || ''
    const sessionId = this.currentSessionId || this.sessionPersistence.getSessionId() || ''

    const body: { answerIndex?: number, textAnswer?: string } = {}
    if (answerIndex !== undefined) {
      body.answerIndex = answerIndex
    }
    if (textAnswer !== undefined) {
      body.textAnswer = textAnswer
    }

    return from(
      client.post<{ 200: void, 201: void, 204: void }>({
        url: '/apiv1/sessions/{id}/answers',
        path: { id: sessionId },
        body,
        headers: { 'X-Participant-Token': token },
      }),
    ).pipe(
      map(({ error }) => {
        if (error) {
          return {
            kind: 'failure' as const,
            error: new ValidationError({}, 'Erreur d\'envoi de la réponse'),
          }
        }
        return { kind: 'success' as const, result: undefined }
      }),
    )
  }

  leaveSession(): ServiceOutput<void> {
    const token = this.currentParticipationToken || this.sessionPersistence.getParticipationToken() || ''
    const sessionId = this.currentSessionId || this.sessionPersistence.getSessionId() || ''

    this.currentParticipationToken = ''
    this.currentSessionId = ''
    this.currentSessionCode = ''

    if (!sessionId || !token) {
      return of({ kind: 'success' as const, result: undefined })
    }

    return from(
      client.post<{ 200: void, 204: void }>({
        url: '/apiv1/sessions/{id}/leave',
        path: { id: sessionId },
        headers: { 'X-Participant-Token': token },
      }),
    ).pipe(
      map(() => ({ kind: 'success' as const, result: undefined })),
    )
  }
}

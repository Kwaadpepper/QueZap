import { HttpClient } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'

import { catchError, from, map, of } from 'rxjs'

import { LeaderboardResponseDto } from '@quezap/core/api-client'
import { getByCode, getScores, join, leaveSession, submitAnswer } from '@quezap/core/api-client/sdk.gen'
import { zLeaderboardResponseDto } from '@quezap/core/api-client/zod.gen'
import {
  ExpiredError,
  ForbidenError,
  NotFoundError,
  ServiceError,
  UnauthorizedError,
  ValidationError,
} from '@quezap/core/errors'
import { ServiceOutput } from '@quezap/core/types'
import { Session, SessionCode, SessionId } from '@quezap/domain/models'

import { ActiveSessionPersistence } from '../active-session-persistence/active-session-persistence'

import { SessionApiService } from './session-api'

@Injectable({ providedIn: 'root' })
export class SessionApiHttpService implements SessionApiService {
  private readonly httpClient = inject(HttpClient)
  private readonly sessionPersistence = inject(ActiveSessionPersistence)

  private currentSessionCode = ''
  private currentParticipationToken = ''
  private currentSessionId = ''

  private extractErrorMessage(error: unknown): string | undefined {
    if (typeof error !== 'object' || error === null) {
      return undefined
    }
    const candidate = error as { detail?: unknown, message?: unknown }
    if (typeof candidate.detail === 'string' && candidate.detail.trim()) {
      return candidate.detail.trim()
    }
    if (typeof candidate.message === 'string' && candidate.message.trim()) {
      return candidate.message.trim()
    }
    return undefined
  }

  private mapSubmitAnswerError(status: number | undefined, error: unknown): ServiceError {
    const serverMessage = this.extractErrorMessage(error)
    const isOffline = status === 0
      || status === undefined
      || (typeof navigator !== 'undefined' && navigator.onLine === false)

    if (isOffline) {
      return new ServiceError(
        'Connexion perdue : impossible d\'envoyer ta réponse. Vérifie ton réseau et réessaie.',
      )
    }

    switch (status) {
      case 409:
        return new ValidationError(
          {},
          serverMessage ?? 'Réponse tardive : les réponses sont déjà verrouillées pour cette question.',
        )
      case 400:
        return new ValidationError(
          {},
          serverMessage ?? 'Temps écoulé : ta réponse n\'a pas été prise en compte.',
        )
      case 403:
        return new ForbidenError('Tu as été exclu de cette session.')
      case 401:
        return new UnauthorizedError('Ta participation n\'est plus valide pour cette session.')
      case 404:
        return new NotFoundError('Cette session est introuvable ou terminée.')
      default:
        return new ServiceError(
          serverMessage ?? 'Impossible d\'enregistrer ta réponse. Réessaie.',
        )
    }
  }

  private resolveSessionStatus(status: string | undefined): Session['status'] {
    if (status === 'LOBBY' || status === 'STARTED' || status === 'ENDED') {
      return status
    }
    return undefined
  }

  joinSessionWith(code: SessionCode): ServiceOutput<Session, NotFoundError | ExpiredError> {
    this.currentSessionCode = (typeof code === 'string' ? code : String(code)).trim().toUpperCase().replace(/\s+/g, '')

    return from(
      getByCode({
        path: { code: this.currentSessionCode },
        httpClient: this.httpClient,
      }),
    ).pipe(
      map(({ data, error, response }) => {
        const status = response?.status
        if (error || !data) {
          if (status === 404) {
            return {
              kind: 'failure' as const,
              error: new NotFoundError('Session introuvable'),
            }
          }
          if (status === 410) {
            return {
              kind: 'failure' as const,
              error: new ExpiredError('Session expirée'),
            }
          }
          if (status === 0 || !response) {
            return {
              kind: 'failure' as const,
              error: new ServiceError('Impossible de joindre le serveur. Vérifie ta connexion.'),
            }
          }
          return {
            kind: 'failure' as const,
            error: new ExpiredError('Session expirée ou erreur serveur'),
          }
        }

        const id = data.id ?? ''
        const storedSessionId = this.sessionPersistence.getSessionId()
        const storedToken = this.sessionPersistence.getParticipationToken()
        const isExistingParticipant = Boolean(id && storedSessionId === id && storedToken)

        if (data.status === 'ENDED' && !isExistingParticipant) {
          return {
            kind: 'failure' as const,
            error: new ExpiredError('Cette session est déjà terminée'),
          }
        }

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
          status: this.resolveSessionStatus(data.status),
          startedAt: data.startedAt
            ? new Date(data.startedAt)
            : (data.status === 'STARTED' || data.status === 'ENDED' ? new Date() : null),
          endedAt: data.endedAt
            ? new Date(data.endedAt)
            : (data.status === 'ENDED' ? new Date() : null),
        }
        return {
          kind: 'success' as const,
          result: session,
        }
      }),
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new ServiceError('Impossible de joindre le serveur. Vérifie ta connexion.'),
        }),
      ),
    )
  }

  chooseNickname(nickname: string): ServiceOutput<string | void, ForbidenError | ValidationError> {
    const rawCode = this.currentSessionCode || this.sessionPersistence.retrieve()?.code || ''
    const code = rawCode.trim().toUpperCase()
    const trimmedNickname = nickname.trim()
    const payload = {
      code,
      participantName: trimmedNickname,
    }

    const existingToken = this.currentParticipationToken || this.sessionPersistence.getParticipationToken()
    const headers: Record<string, string> | undefined = existingToken
      ? { 'X-Participant-Token': existingToken }
      : undefined

    return from(
      join({
        body: payload,
        headers,
        httpClient: this.httpClient,
      }),
    ).pipe(
      map(({ data, error, response }) => {
        const status = response?.status
        if (error || !data) {
          if (status === 409 || status === 400) {
            const serverMessage = this.extractErrorMessage(error)
            const detail
              = status === 409
                ? 'Ce pseudo est déjà utilisé dans ce salon. Choisis un autre prénom ou ajoute une initiale.'
                : serverMessage || 'Pseudo déjà utilisé ou invalide'
            return {
              kind: 'failure' as const,
              error: new ValidationError({
                nickname: [detail],
                nicknameValue: [detail],
              }),
            }
          }
          if (status === 403) {
            return {
              kind: 'failure' as const,
              error: new ForbidenError('Impossible de rejoindre la session'),
            }
          }
          if (status === 0 || !response) {
            return {
              kind: 'failure' as const,
              error: new ServiceError('Connexion perdue. Vérifie ton réseau et réessaie.'),
            }
          }
          return {
            kind: 'failure' as const,
            error: new ForbidenError('Impossible de rejoindre la session'),
          }
        }

        const token = data.participationToken ?? ''
        const sessId = data.sessionId ?? ''
        const canonicalName = (data.participantName || trimmedNickname).trim()
        this.currentParticipationToken = token
        this.currentSessionId = sessId
        this.sessionPersistence.saveParticipationData(sessId, token, canonicalName)
        return {
          kind: 'success' as const,
          result: canonicalName,
        }
      }),
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new ServiceError('Connexion perdue. Vérifie ton réseau et réessaie.'),
        }),
      ),
    )
  }

  submitAnswer(answerIndex?: number, textAnswer?: string): ServiceOutput<void> {
    const token = this.currentParticipationToken || this.sessionPersistence.getParticipationToken() || ''
    const sessionId = this.currentSessionId || this.sessionPersistence.getSessionId() || ''

    if (!sessionId || !token) {
      return of({
        kind: 'failure' as const,
        error: new UnauthorizedError('Ta participation n\'est plus valide pour cette session.'),
      })
    }

    const body: { answerIndex?: number, textAnswer?: string } = {}
    if (answerIndex !== undefined) {
      body.answerIndex = answerIndex
    }
    if (textAnswer !== undefined) {
      body.textAnswer = textAnswer
    }

    return from(
      submitAnswer({
        path: { id: sessionId },
        body,
        headers: { 'X-Participant-Token': token },
        httpClient: this.httpClient,
      }),
    ).pipe(
      map(({ error, response }) => {
        const status = response?.status
        if (error) {
          return {
            kind: 'failure' as const,
            error: this.mapSubmitAnswerError(status, error),
          }
        }
        return { kind: 'success' as const, result: undefined }
      }),
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new ServiceError(
            'Connexion perdue : impossible d\'envoyer ta réponse. Vérifie ton réseau et réessaie.',
          ),
        }),
      ),
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
      leaveSession({
        path: { id: sessionId },
        headers: { 'X-Participant-Token': token },
        httpClient: this.httpClient,
      }),
    ).pipe(
      map(() => ({ kind: 'success' as const, result: undefined })),
      catchError(() => of({ kind: 'success' as const, result: undefined })),
    )
  }

  getSessionScores(sessionId: string): ServiceOutput<LeaderboardResponseDto> {
    if (!sessionId) {
      return of({
        kind: 'failure' as const,
        error: new NotFoundError('Identifiant de session manquant.'),
      })
    }

    const token = this.currentParticipationToken || this.sessionPersistence.getParticipationToken()

    return from(
      getScores({
        path: { id: sessionId },
        ...(token ? { headers: { 'X-Participant-Token': token } } : {}),
        httpClient: this.httpClient,
      }),
    ).pipe(
      map(({ data, error, response }) => {
        const status = response?.status
        if (error || !data) {
          if (status === 404) {
            return {
              kind: 'failure' as const,
              error: new NotFoundError('Classement introuvable pour cette session.'),
            }
          }
          return {
            kind: 'failure' as const,
            error: new ServiceError('Impossible de récupérer les scores finaux.'),
          }
        }

        const parsed = zLeaderboardResponseDto.safeParse(data)
        if (!parsed.success) {
          return {
            kind: 'failure' as const,
            error: new ServiceError('Format de classement invalide.'),
          }
        }

        return {
          kind: 'success' as const,
          result: parsed.data,
        }
      }),
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new ServiceError('Impossible de récupérer les scores finaux.'),
        }),
      ),
    )
  }
}

import { inject, Injectable, signal } from '@angular/core'

import { delay, map, of, tap } from 'rxjs'

import { ExpiredError, ForbidenError, NotFoundError, ServiceError, ValidationError } from '@quezap/core/errors'
import { ServiceOutput } from '@quezap/core/types'
import { Session, SessionCode, sessionHasEnded } from '@quezap/domain/models'

import { SessionMocks } from '../session.mock'

import { SessionApiService } from './session-api'

@Injectable()
export class SessionApiMockService implements SessionApiService {
  private readonly MOCK_ERROR = (failureProbability = 0.2) => Math.random() < failureProbability
  private readonly MOCK_DELAY = () => Math.max(2000, Math.random() * 5000)
  readonly #validPseudos = ['Alice', 'Bob', 'Charlie', 'Diana', 'Eve', 'Frank', 'Grace', 'Hannah']

  private readonly currentSession = signal<Session | null>(null)

  private readonly sessions = inject(SessionMocks)

  joinSessionWith(code: SessionCode): ServiceOutput<Session, NotFoundError | ExpiredError> {
    return of(code).pipe(
      delay(this.MOCK_DELAY()),
      tap(() => {
        if (this.MOCK_ERROR()) {
          console.debug('Mock: error while joining session')
          throw new ServiceError('Mock service error: find session')
        }
      }),
      map((code) => {
        const session = this.sessions.getSessionByCode(code)
        if (!session) {
          console.debug('Mock: session not found')
          return new NotFoundError(`Session with code ${code} not found`)
        }

        if (sessionHasEnded(session)) {
          console.debug('Mock: session has expired')
          return new ExpiredError('Session has expired')
        }

        this.currentSession.set(session)

        console.log('Mock session joined:', session)
        return {
          kind: 'success',
          result: session,
        }
      }),
    )
  }

  chooseNickname(nickname: string): ServiceOutput<void, ForbidenError | ValidationError> {
    return of(nickname).pipe(
      delay(this.MOCK_DELAY()),
      tap(() => {
        if (this.MOCK_ERROR()) {
          console.debug('Mock: error while choosing nickname')
          throw new ServiceError('Mock service error: choose nickname')
        }
      }),
      map((nickname) => {
        const errors: Record<string, string[]> = {}

        if (!this.#validPseudos.includes(nickname)) {
          errors['nicknameValue'] = ['Le pseudo choisi est déjà utilisé. Choisissez-en un autre.']
        }

        if (nickname.includes('crotte')) {
          errors['nicknameValue'] = [
            ...(errors['nicknameValue'] ?? []),
            'Le pseudo choisi contient un mot interdit. Choisissez-en un autre.',
          ]
        }

        if (Object.keys(errors).length > 0) {
          console.debug('Mock: validation error on nickname', errors)
          return new ValidationError(errors, 'Validation failed')
        }

        const session = this.currentSession()
        if (session === null) {
          console.debug('Mock: no active session to join with nickname')
          return new ForbidenError('No active session to join with nickname')
        }

        this.sessions.addParticipantToSession(session.code, nickname)

        return {
          kind: 'success',
          result: undefined,
        }
      }),
    )
  }
}

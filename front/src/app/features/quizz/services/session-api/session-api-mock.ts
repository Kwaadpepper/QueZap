import { inject, Injectable } from '@angular/core'

import { delay, map, of, tap } from 'rxjs'

import { NotFoundError, ServiceError, ValidationError } from '@quezap/core/errors'
import { ServiceOutput } from '@quezap/core/types'
import { Session, SessionCode } from '@quezap/domain/models'

import { SessionMocks } from '../session.mock'

import { SessionApiService } from './session-api'

@Injectable()
export class SessionApiMockService implements SessionApiService {
  private readonly MOCK_ERROR = (failureProbability = 0.2) => Math.random() < failureProbability
  private readonly MOCK_DELAY = () => Math.max(2000, Math.random() * 5000)
  readonly #validPseudos = ['Alice', 'Bob', 'Charlie', 'Diana', 'Eve', 'Frank', 'Grace', 'Hannah']

  private readonly sessions = inject(SessionMocks)

  find(code: SessionCode): ServiceOutput<Session, NotFoundError> {
    return of(code).pipe(
      delay(this.MOCK_DELAY()),
      tap(() => {
        if (this.MOCK_ERROR()) {
          throw new ServiceError('Mock service error: find session')
        }
      }),
      map((code) => {
        const session = this.sessions.getSessionByCode(code)
        if (!session) {
          return new NotFoundError(`Session with code ${code} not found`)
        }

        console.log('Mock session found:', session)
        return {
          kind: 'success',
          result: session,
        }
      }),
    )
  }

  chooseNickname(nickname: string): ServiceOutput<void, ValidationError> {
    return of(nickname).pipe(
      delay(this.MOCK_DELAY()),
      tap(() => {
        if (this.MOCK_ERROR()) {
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
          return new ValidationError(errors, 'Validation failed')
        }

        return {
          kind: 'success',
          result: undefined,
        }
      }),
    )
  }
}

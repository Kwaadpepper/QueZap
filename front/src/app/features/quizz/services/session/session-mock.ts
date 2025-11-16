import { Injectable, signal } from '@angular/core'

import { delay, map, of, tap } from 'rxjs'

import { NotFoundError, ServiceError } from '@quezap/core/errors'
import { ServiceOutput } from '@quezap/core/types'
import { Session, SessionCode } from '@quezap/domain/models'

import { SessionService } from './session'
import { MOCK_SESSIONS } from './session.mock'

@Injectable()
export class SessionMockService implements SessionService {
  private readonly MOCK_ERROR = (failureProbability = 0.2) => Math.random() < failureProbability
  private readonly MOCK_DELAY = () => Math.max(2000, Math.random() * 5000)

  private readonly sessions = signal(MOCK_SESSIONS)

  find(code: SessionCode): ServiceOutput<Session, NotFoundError> {
    return of(code).pipe(
      delay(this.MOCK_DELAY()),
      tap(() => {
        if (this.MOCK_ERROR()) {
          throw new ServiceError('Mock service error')
        }
      }),
      map((code) => {
        const session = this.sessions().find(s => s.code === code)
        if (!session) {
          return new NotFoundError(`Session with code ${code} not found`)
        }
        return {
          kind: 'success',
          result: session,
        }
      }),
    )
  }
}

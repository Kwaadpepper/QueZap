import { InjectionToken } from '@angular/core'

import { NotFoundError } from '@quezap/core/errors'
import { ServiceOutput } from '@quezap/core/types'
import { Session, SessionCode } from '@quezap/domain/models'

export interface SessionService {
  find(code: SessionCode): ServiceOutput<Session, NotFoundError>
}

export const SESSION_SERVICE = new InjectionToken<SessionService>('SessionService')

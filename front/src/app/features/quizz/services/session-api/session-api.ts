import { InjectionToken } from '@angular/core'

import { ExpiredError, ForbidenError, NotFoundError, ValidationError } from '@quezap/core/errors'
import { ServiceOutput } from '@quezap/core/types'
import { Session, SessionCode } from '@quezap/domain/models'

export interface SessionApiService {
  joinSessionWith(code: SessionCode): ServiceOutput<Session, NotFoundError | ExpiredError>

  chooseNickname(nickname: string): ServiceOutput<void, ForbidenError | ValidationError>
}

export const SESSION_API_SERVICE = new InjectionToken<SessionApiService>('SessionApiService')

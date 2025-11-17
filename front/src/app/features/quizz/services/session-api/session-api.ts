import { InjectionToken } from '@angular/core'

import { NotFoundError, ValidationError } from '@quezap/core/errors'
import { ServiceOutput } from '@quezap/core/types'
import { Session, SessionCode } from '@quezap/domain/models'

export interface SessionApiService {
  find(code: SessionCode): ServiceOutput<Session, NotFoundError>

  chooseNickname(nickname: string): ServiceOutput<void, ValidationError>
}

export const SESSION_API_SERVICE = new InjectionToken<SessionApiService>('SessionApiService')

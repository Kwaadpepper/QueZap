import { InjectionToken } from '@angular/core'

import { PageOf, Pagination, ServiceOutput } from '@quezap/core/types'
import { Session } from '@quezap/domain/models'

export interface SessionService {
  getSessionPage(page: Pagination): ServiceOutput<PageOf<Session>>
}

export const SESSION_SERVICE = new InjectionToken<SessionService>('SessionService')

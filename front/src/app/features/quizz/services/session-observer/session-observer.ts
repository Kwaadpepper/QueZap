import { InjectionToken } from '@angular/core'

import { ServiceObservable } from '@quezap/core/types'
import { Participant } from '@quezap/domain/models'

export interface SessionObserverService {
  participants(): ServiceObservable<Participant[]>
}

export const SESSION_OBSERVER_SERVICE = new InjectionToken<SessionObserverService>('SessionObserverService')

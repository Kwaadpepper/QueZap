import { InjectionToken } from '@angular/core'

import { ServiceObservable } from '@quezap/core/types'
import { MixedQuestion, Participant } from '@quezap/domain/models'

export interface NoMoreQuestions {
  readonly type: 'NoMoreQuestions'
}

export interface SessionObserverService {
  participants(): ServiceObservable<Participant[]>

  questions(): ServiceObservable<MixedQuestion | NoMoreQuestions>
}

export const SESSION_OBSERVER_SERVICE = new InjectionToken<SessionObserverService>('SessionObserverService')

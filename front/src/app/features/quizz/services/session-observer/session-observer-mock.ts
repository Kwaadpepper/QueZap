import { Injectable } from '@angular/core'

import { delay, map, of, tap } from 'rxjs'

import { ServiceError } from '@quezap/core/errors'
import { ServiceObservable } from '@quezap/core/types'
import { Participant, ParticipantId } from '@quezap/domain/models'

import { SessionObserverService } from './session-observer'

@Injectable()
export class SessionObserverMockService implements SessionObserverService {
  private readonly MOCK_ERROR = (failureProbability = 0.2) => Math.random() < failureProbability
  private readonly MOCK_DELAY = () => Math.max(2000, Math.random() * 5000)
  readonly #participantsNames = [
    'John', 'Jerry', 'Ivy', 'Jack', 'Kathy', 'Liam',
    'Mia', 'Noah', 'Olivia', 'Paul', 'Quinn', 'Ruby',
  ]

  private readonly mockParticipants: Participant[] = this.#participantsNames.map(nickname => ({
    id: crypto.randomUUID() as ParticipantId,
    nickname,
    score: Math.floor(Math.random() * 100) as unknown as Participant['score'],
  }))

  participants(): ServiceObservable<Participant[]> {
    return of(this.mockParticipants).pipe(
      delay(this.MOCK_DELAY()),
      tap(() => {
        if (this.MOCK_ERROR()) {
          throw new ServiceError('Mock service error')
        }
      }),
      map(participants => ({
        kind: 'success',
        result: participants,
      })),
    )
  }
}

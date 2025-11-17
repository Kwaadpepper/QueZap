import { UUID } from '../types'

export type ParticipantId = UUID & {
  readonly __type: 'Participant'
}

export type Score = number & {
  readonly __type: 'Score'
}

export interface Participant {
  readonly id: ParticipantId
  readonly nickname: string
  readonly score: Score
}

import { UUID } from '../types'

export type UserId = UUID & {
  readonly __type: 'UserId'
}

export interface AuthenticatedUser {
  readonly id: UserId
  readonly pseudo: string
}

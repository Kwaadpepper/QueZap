import { UUID } from '../types'

export interface AuthenticatedUser {
  readonly uuid: UUID
  readonly pseudo: string
}

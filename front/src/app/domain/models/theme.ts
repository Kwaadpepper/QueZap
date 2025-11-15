import { UUID } from '../types'

export type ThemeId = UUID & {
  readonly __type: 'ThemeId'
}

export interface Theme {
  readonly id: ThemeId
  readonly name: string
}

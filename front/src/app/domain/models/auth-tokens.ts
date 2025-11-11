import { JWT } from '../types'

export interface AuthTokens {
  readonly accessToken: JWT
  readonly refreshToken: JWT
}

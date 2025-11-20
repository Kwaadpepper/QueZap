import { Injectable } from '@angular/core'

import { AuthTokens } from '@quezap/domain/models'

@Injectable({ providedIn: 'root' })
export class TokenPersitance {
  readonly #TOKEN_KEY = 'auth_tokens'

  private tokenKey() {
    return this.#TOKEN_KEY
  }

  public saveTokens(tokens: AuthTokens): void {
    try {
      localStorage.setItem(this.tokenKey(), JSON.stringify(tokens))
    }
    catch (e) {
      console.error('Could not save tokens to localStorage', e)
    }
  }

  public getTokens(): AuthTokens | undefined {
    try {
      const tokens = localStorage.getItem(this.tokenKey())
      return tokens ? (JSON.parse(tokens) as AuthTokens) : undefined
    }
    catch (e) {
      console.error('Could not read tokens from localStorage', e)
      return undefined
    }
  }

  public removeTokens(): void {
    localStorage.removeItem(this.tokenKey())
  }
}

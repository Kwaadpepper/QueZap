import { Injectable } from '@angular/core'

import { AuthTokens } from '@quezap/domain/models'

@Injectable({
  providedIn: 'root',
})
export class TokenPersitance {
  readonly #TOKEN_KEY = 'auth_tokens'

  private tokenKey() {
    return this.#TOKEN_KEY
  }

  public saveTokens(tokens: AuthTokens): void {
    try {
      sessionStorage.setItem(this.tokenKey(), JSON.stringify(tokens))
    }
    catch (e) {
      console.error('Could not save tokens to sessionStorage', e)
    }
  }

  public getTokens(): AuthTokens | undefined {
    try {
      const tokens = sessionStorage.getItem(this.tokenKey())
      return tokens ? (JSON.parse(tokens) as AuthTokens) : undefined
    }
    catch (e) {
      console.error('Could not read tokens from sessionStorage', e)
      return undefined
    }
  }

  public removeTokens(): void {
    sessionStorage.removeItem(this.tokenKey())
  }
}

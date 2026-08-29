import { Injectable } from '@angular/core'

import { AuthTokens } from '@quezap/domain/models'

@Injectable({ providedIn: 'root' })
export class TokenPersitance {
  readonly #TOKEN_KEY = 'auth_tokens'
  readonly #memoryCache = new Map<string, string>()

  private tokenKey() {
    return this.#TOKEN_KEY
  }

  private get storage(): Storage | undefined {
    return typeof localStorage !== 'undefined' ? localStorage : undefined
  }

  public saveTokens(tokens: AuthTokens): void {
    const serialized = JSON.stringify(tokens)
    this.#memoryCache.set(this.tokenKey(), serialized)
    try {
      this.storage?.setItem(this.tokenKey(), serialized)
    }
    catch (e) {
      console.error('Could not save tokens to localStorage', e)
    }
  }

  public getTokens(): AuthTokens | undefined {
    try {
      const tokens = this.storage?.getItem(this.tokenKey())
      if (tokens) {
        return JSON.parse(tokens) as AuthTokens
      }
    }
    catch (e) {
      console.error('Could not read tokens from localStorage', e)
    }

    const fallback = this.#memoryCache.get(this.tokenKey())
    if (fallback) {
      try {
        return JSON.parse(fallback) as AuthTokens
      }
      catch {
        return undefined
      }
    }

    return undefined
  }

  public removeTokens(): void {
    this.#memoryCache.delete(this.tokenKey())
    try {
      this.storage?.removeItem(this.tokenKey())
    }
    catch (e) {
      console.error('Could not remove tokens from localStorage', e)
    }
  }
}

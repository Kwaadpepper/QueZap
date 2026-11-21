import { Injectable } from '@angular/core'

import { AuthTokens } from '@quezap/domain/models'

@Injectable({ providedIn: 'root' })
export class TokenPersitance {
  #tokens: AuthTokens | undefined

  public saveTokens(tokens: AuthTokens): void {
    this.#tokens = { ...tokens }
  }

  public getTokens(): AuthTokens | undefined {
    return this.#tokens ? { ...this.#tokens } : undefined
  }

  public removeTokens(): void {
    this.#tokens = undefined
  }
}

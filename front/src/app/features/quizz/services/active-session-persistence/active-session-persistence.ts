import { Injectable } from '@angular/core'

import { SessionCode } from '@quezap/domain/models'

@Injectable({
  providedIn: 'root',
})
export class ActiveSessionPersistence {
  readonly #storageKey = 'activeSessionCode'

  persists(code: SessionCode): void {
    localStorage.setItem(this.#storageKey, code)
  }

  retrieve(): SessionCode | null {
    const code = localStorage.getItem(this.#storageKey)
    return code ? (code as SessionCode) : null
  }

  clear(): void {
    localStorage.removeItem(this.#storageKey)
  }
}

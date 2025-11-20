import { Injectable } from '@angular/core'

import { SessionCode } from '@quezap/domain/models'

export interface PersistedSessionData {
  code: SessionCode
  nickname?: string
}

@Injectable({ providedIn: 'root' })
export class ActiveSessionPersistence {
  readonly #storageKey = 'activeSessionCode'

  persist(data: PersistedSessionData): void {
    localStorage.setItem(this.#storageKey, JSON.stringify(data))
  }

  patch(data: PersistedSessionData): void {
    const existingData = this.retrieve() ?? {}
    const mergedData = { ...existingData, ...data }
    this.persist(mergedData)
  }

  persistNickname(nickname?: string): void {
    const existingData = this.retrieve() ?? { code: '' as SessionCode }
    existingData.nickname = nickname
    this.patch(existingData)
  }

  retrieve(): PersistedSessionData | null {
    try {
      const item = localStorage.getItem(this.#storageKey)

      if (item === null) {
        return null
      }

      return JSON.parse(item)
    }
    catch {
      return null
    }
  }

  clear(): void {
    localStorage.removeItem(this.#storageKey)
  }

  clearKeepingNickname(): void {
    const existingData = this.retrieve()
    if (existingData?.nickname) {
      this.persist({ code: '' as SessionCode, nickname: existingData.nickname })
    }
    else {
      this.clear()
    }
  }
}

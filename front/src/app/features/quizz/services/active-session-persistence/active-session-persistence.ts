import { Injectable, signal } from '@angular/core'

import { SessionCode } from '@quezap/domain/models'

export interface PersistedSessionData {
  code: SessionCode
  nickname?: string
  sessionId?: string
  participationToken?: string
}

export interface PersistedSubmittedChoice {
  questionId: string
  answerIndex: number
  label: string
}

export interface FinalParticipantResults {
  rank: number
  totalParticipants: number
  score: number
  correctAnswers: number
  isPodium: boolean
  classAverageScore?: number
  classSuccessRate?: number
  fullLeaderboard?: {
    rank: number
    participantName: string
    score: number
    correctCount?: number
  }[]
}

@Injectable({ providedIn: 'root' })
export class ActiveSessionPersistence {
  readonly #storageKey = 'activeSessionCode'
  readonly #participationTokenKey = 'quezap_participation_token'
  readonly #sessionIdKey = 'quezap_session_id'
  readonly #participantNameKey = 'quezap_participant_name'
  readonly #finalResultsKey = 'quezap_final_results'
  readonly #submittedChoiceKey = 'quezap_submitted_choice'

  // Resilient memory cache in case browser storage is restricted or throws
  readonly #memoryCache = new Map<string, string>()

  readonly #participantNameSignal = signal<string | null>(null)
  readonly #finalResultsSignal = signal<FinalParticipantResults | null>(null)

  private setSafeItem(storage: Storage | undefined, key: string, value: string): void {
    this.#memoryCache.set(key, value)
    try {
      if (storage) {
        storage.setItem(key, value)
      }
    }
    catch (e) {
      console.warn(`Could not save item "${key}" to storage`, e)
    }
  }

  private getSafeItem(storage: Storage | undefined, key: string): string | null {
    try {
      if (storage) {
        const val = storage.getItem(key)
        if (val !== null) {
          return val
        }
      }
    }
    catch (e) {
      console.warn(`Could not read item "${key}" from storage`, e)
    }
    return this.#memoryCache.get(key) ?? null
  }

  private removeSafeItem(storage: Storage | undefined, key: string): void {
    this.#memoryCache.delete(key)
    try {
      if (storage) {
        storage.removeItem(key)
      }
    }
    catch (e) {
      console.warn(`Could not remove item "${key}" from storage`, e)
    }
  }

  private get localStorageRef(): Storage | undefined {
    return typeof localStorage !== 'undefined' ? localStorage : undefined
  }

  private get sessionStorageRef(): Storage | undefined {
    return typeof sessionStorage !== 'undefined' ? sessionStorage : undefined
  }

  persist(data: PersistedSessionData): void {
    this.setSafeItem(this.localStorageRef, this.#storageKey, JSON.stringify(data))
    if (data.sessionId) {
      this.setSessionId(data.sessionId)
    }
    if (data.participationToken) {
      this.setParticipationToken(data.participationToken)
    }
    if (data.nickname) {
      this.setSafeItem(this.sessionStorageRef, this.#participantNameKey, data.nickname)
      this.#participantNameSignal.set(data.nickname)
    }
  }

  patch(data: Partial<PersistedSessionData>): void {
    const existingData = this.retrieve() ?? { code: '' as SessionCode }
    const mergedData: PersistedSessionData = {
      ...existingData,
      ...data,
      code: (data.code ?? existingData.code ?? '') as SessionCode,
    }
    this.persist(mergedData)
  }

  persistNickname(nickname?: string): void {
    const existingData = this.retrieve() ?? { code: '' as SessionCode }
    existingData.nickname = nickname
    this.patch(existingData)
  }

  retrieve(): PersistedSessionData | null {
    try {
      const item = this.getSafeItem(this.localStorageRef, this.#storageKey)

      if (item === null) {
        return null
      }

      const parsed = JSON.parse(item) as PersistedSessionData
      const token = this.getParticipationToken()
      const sessionId = this.getSessionId()
      if (token && !parsed.participationToken) {
        parsed.participationToken = token
      }
      if (sessionId && !parsed.sessionId) {
        parsed.sessionId = sessionId
      }
      return parsed
    }
    catch {
      return null
    }
  }

  saveParticipationData(sessionId: string, participationToken: string, nickname?: string): void {
    this.setParticipationToken(participationToken)
    this.setSessionId(sessionId)
    if (nickname) {
      this.setSafeItem(this.sessionStorageRef, this.#participantNameKey, nickname)
      this.#participantNameSignal.set(nickname)
    }
    const current = this.retrieve() ?? { code: '' as SessionCode }
    this.patch({
      ...current,
      sessionId,
      participationToken,
      ...(nickname ? { nickname } : {}),
    })
  }

  getParticipationToken(): string | null {
    return this.getSafeItem(this.localStorageRef, this.#participationTokenKey)
      ?? this.getSafeItem(this.sessionStorageRef, this.#participationTokenKey)
  }

  setParticipationToken(token: string): void {
    this.setSafeItem(this.localStorageRef, this.#participationTokenKey, token)
    this.setSafeItem(this.sessionStorageRef, this.#participationTokenKey, token)
  }

  getSessionId(): string | null {
    return this.getSafeItem(this.localStorageRef, this.#sessionIdKey)
      ?? this.getSafeItem(this.sessionStorageRef, this.#sessionIdKey)
  }

  setSessionId(sessionId: string): void {
    this.setSafeItem(this.localStorageRef, this.#sessionIdKey, sessionId)
    this.setSafeItem(this.sessionStorageRef, this.#sessionIdKey, sessionId)
  }

  getParticipantName(): string | null {
    const sigVal = this.#participantNameSignal()
    const stored = this.getSafeItem(this.localStorageRef, this.#participantNameKey)
      ?? this.getSafeItem(this.sessionStorageRef, this.#participantNameKey)
    return stored ?? sigVal
  }

  saveSubmittedChoice(questionId: string, choice: { answerIndex: number, label: string }): void {
    if (!questionId) return
    const payload: PersistedSubmittedChoice = {
      questionId,
      answerIndex: choice.answerIndex,
      label: choice.label,
    }
    this.setSafeItem(this.sessionStorageRef, this.#submittedChoiceKey, JSON.stringify(payload))
  }

  getSubmittedChoice(questionId: string): { answerIndex: number, label: string } | undefined {
    if (!questionId) return undefined
    const raw = this.getSafeItem(this.sessionStorageRef, this.#submittedChoiceKey)
    if (!raw) return undefined
    try {
      const parsed = JSON.parse(raw) as PersistedSubmittedChoice
      if (parsed.questionId === questionId) {
        return { answerIndex: parsed.answerIndex, label: parsed.label }
      }
      return undefined
    }
    catch {
      return undefined
    }
  }

  clearSubmittedChoice(): void {
    this.removeSafeItem(this.sessionStorageRef, this.#submittedChoiceKey)
  }

  clearParticipationAuth(): void {
    this.removeSafeItem(this.localStorageRef, this.#participationTokenKey)
    this.removeSafeItem(this.sessionStorageRef, this.#participationTokenKey)
    this.removeSafeItem(this.localStorageRef, this.#participantNameKey)
    this.removeSafeItem(this.sessionStorageRef, this.#participantNameKey)
    this.removeSafeItem(this.localStorageRef, this.#finalResultsKey)
    this.removeSafeItem(this.sessionStorageRef, this.#finalResultsKey)
    this.removeSafeItem(this.sessionStorageRef, this.#submittedChoiceKey)
    this.#participantNameSignal.set(null)
    this.#finalResultsSignal.set(null)
    const existing = this.retrieve()
    if (existing) {
      delete existing.participationToken
      this.setSafeItem(this.localStorageRef, this.#storageKey, JSON.stringify(existing))
    }
  }

  saveFinalResults(results: FinalParticipantResults): void {
    this.setSafeItem(this.sessionStorageRef, this.#finalResultsKey, JSON.stringify(results))
    this.#finalResultsSignal.set(results)
  }

  getFinalResults(): FinalParticipantResults | null {
    const sigVal = this.#finalResultsSignal()
    const raw = this.getSafeItem(this.sessionStorageRef, this.#finalResultsKey)
    if (!raw) return sigVal
    try {
      return JSON.parse(raw) as FinalParticipantResults
    }
    catch {
      return sigVal
    }
  }

  clear(): void {
    this.removeSafeItem(this.localStorageRef, this.#storageKey)
    this.removeSafeItem(this.localStorageRef, this.#participationTokenKey)
    this.removeSafeItem(this.localStorageRef, this.#sessionIdKey)
    this.removeSafeItem(this.localStorageRef, this.#participantNameKey)
    this.removeSafeItem(this.localStorageRef, this.#finalResultsKey)
    this.removeSafeItem(this.sessionStorageRef, this.#participationTokenKey)
    this.removeSafeItem(this.sessionStorageRef, this.#sessionIdKey)
    this.removeSafeItem(this.sessionStorageRef, this.#participantNameKey)
    this.removeSafeItem(this.sessionStorageRef, this.#finalResultsKey)
    this.removeSafeItem(this.sessionStorageRef, this.#submittedChoiceKey)
    this.#participantNameSignal.set(null)
    this.#finalResultsSignal.set(null)
  }

  clearKeepingNickname(): void {
    const existingData = this.retrieve()
    const nickname = existingData?.nickname
    this.clear()
    if (nickname) {
      this.persist({ code: '' as SessionCode, nickname })
    }
  }
}

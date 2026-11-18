import { Injectable, signal } from '@angular/core'

import { SessionCode } from '@quezap/domain/models'

export interface PersistedSessionData {
  code: SessionCode
  nickname?: string
  sessionId?: string
  participationToken?: string
}

export interface PersistedSubmittedChoice {
  sessionId: string
  questionId: string
  answerIndex: number
  label: string
  expiresAt: number
}

export interface FinalParticipantResults {
  rank: number
  totalParticipants: number
  score: number
  correctAnswers?: number
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

interface PersistedFinalResultsEnvelope {
  sessionId: string
  results: FinalParticipantResults
  expiresAt: number
}

interface PersistedParticipantNameEnvelope {
  sessionId: string
  nickname: string
  expiresAt: number
}

const DEFAULT_SESSION_SCOPE = 'default'
const PERSISTENCE_TTL_MS = 24 * 60 * 60 * 1000

@Injectable({ providedIn: 'root' })
export class ActiveSessionPersistence {
  readonly #storageKey = 'activeSessionCode'
  readonly #participationTokenKey = 'quezap_participation_token'
  readonly #sessionIdKey = 'quezap_session_id'
  readonly #participantNameKey = 'quezap_participant_name'
  readonly #finalResultsKey = 'quezap_final_results'
  readonly #submittedChoiceKey = 'quezap_submitted_choice'

  readonly #participantNameKeyPrefix = 'quezap_participant_name:'
  readonly #finalResultsKeyPrefix = 'quezap_final_results:'
  readonly #submittedChoiceKeyPrefix = 'quezap_submitted_choice:'

  // Resilient memory cache in case browser storage is restricted or throws
  readonly #memoryCache = new Map<string, string>()

  readonly #participantNameSignal = signal<string | null>(null)
  readonly #finalResultsSignal = signal<{ sessionId: string, results: FinalParticipantResults } | null>(null)

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

  private resolveSessionScope(sessionId?: string): string {
    return sessionId?.trim() || this.getSessionId()?.trim() || DEFAULT_SESSION_SCOPE
  }

  private isExpired(expiresAt: unknown): boolean {
    return typeof expiresAt === 'number' && Date.now() > expiresAt
  }

  private removeKeysByPrefix(prefix: string): void {
    for (const key of [...this.#memoryCache.keys()]) {
      if (key.startsWith(prefix)) {
        this.#memoryCache.delete(key)
      }
    }
    const storage = this.localStorageRef
    if (!storage) {
      return
    }
    try {
      const keysToRemove: string[] = []
      for (let i = 0; i < storage.length; i++) {
        const key = storage.key(i)
        if (key && key.startsWith(prefix)) {
          keysToRemove.push(key)
        }
      }
      for (const key of keysToRemove) {
        storage.removeItem(key)
      }
    }
    catch (e) {
      console.warn(`Could not remove keys with prefix "${prefix}" from storage`, e)
    }
  }

  public cleanupExpiredEntries(): void {
    const storage = this.localStorageRef
    if (!storage) {
      return
    }
    const scopedPrefixes = [
      this.#submittedChoiceKeyPrefix,
      this.#finalResultsKeyPrefix,
      this.#participantNameKeyPrefix,
    ]
    try {
      const expiredKeys: string[] = []
      for (let i = 0; i < storage.length; i++) {
        const key = storage.key(i)
        if (!key || !scopedPrefixes.some(prefix => key.startsWith(prefix))) {
          continue
        }
        const raw = storage.getItem(key)
        if (!raw) {
          continue
        }
        try {
          const parsed = JSON.parse(raw) as { expiresAt?: unknown }
          if (this.isExpired(parsed.expiresAt)) {
            expiredKeys.push(key)
          }
        }
        catch {
          expiredKeys.push(key)
        }
      }
      for (const key of expiredKeys) {
        this.removeSafeItem(storage, key)
      }
    }
    catch (e) {
      console.warn('Could not clean up expired session storage entries', e)
    }
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
      const scope = this.resolveSessionScope(data.sessionId)
      const envelope: PersistedParticipantNameEnvelope = {
        sessionId: scope,
        nickname: data.nickname,
        expiresAt: Date.now() + PERSISTENCE_TTL_MS,
      }
      this.setSafeItem(this.localStorageRef, `${this.#participantNameKeyPrefix}${scope}`, JSON.stringify(envelope))
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
    this.cleanupExpiredEntries()
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
      const envelope: PersistedParticipantNameEnvelope = {
        sessionId,
        nickname,
        expiresAt: Date.now() + PERSISTENCE_TTL_MS,
      }
      this.setSafeItem(this.localStorageRef, `${this.#participantNameKeyPrefix}${sessionId}`, JSON.stringify(envelope))
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
    const previousSessionId = this.getSessionId()
    if (previousSessionId && previousSessionId !== sessionId) {
      this.removeSafeItem(this.localStorageRef, `${this.#submittedChoiceKeyPrefix}${previousSessionId}`)
      this.removeSafeItem(this.localStorageRef, `${this.#finalResultsKeyPrefix}${previousSessionId}`)
      this.removeSafeItem(this.localStorageRef, `${this.#participantNameKeyPrefix}${previousSessionId}`)
    }
    this.setSafeItem(this.localStorageRef, this.#sessionIdKey, sessionId)
    this.setSafeItem(this.sessionStorageRef, this.#sessionIdKey, sessionId)
  }

  getParticipantName(sessionId?: string): string | null {
    const sigVal = this.#participantNameSignal()
    const scope = this.resolveSessionScope(sessionId)
    const scopedKey = `${this.#participantNameKeyPrefix}${scope}`
    const rawScoped = this.getSafeItem(this.localStorageRef, scopedKey)
    if (rawScoped) {
      try {
        const parsed = JSON.parse(rawScoped) as PersistedParticipantNameEnvelope
        if (!this.isExpired(parsed.expiresAt) && parsed.sessionId === scope && parsed.nickname) {
          return parsed.nickname
        }
        this.removeSafeItem(this.localStorageRef, scopedKey)
      }
      catch {
        this.removeSafeItem(this.localStorageRef, scopedKey)
      }
    }
    const stored = this.getSafeItem(this.localStorageRef, this.#participantNameKey)
      ?? this.getSafeItem(this.sessionStorageRef, this.#participantNameKey)
    return stored ?? sigVal
  }

  saveSubmittedChoice(questionId: string, choice: { answerIndex: number, label: string }, sessionId?: string): void {
    if (!questionId) return
    const scope = this.resolveSessionScope(sessionId)
    const payload: PersistedSubmittedChoice = {
      sessionId: scope,
      questionId,
      answerIndex: choice.answerIndex,
      label: choice.label,
      expiresAt: Date.now() + PERSISTENCE_TTL_MS,
    }
    this.setSafeItem(this.localStorageRef, `${this.#submittedChoiceKeyPrefix}${scope}`, JSON.stringify(payload))
  }

  getSubmittedChoice(questionId: string, sessionId?: string): { answerIndex: number, label: string } | undefined {
    if (!questionId) return undefined
    const scope = this.resolveSessionScope(sessionId)
    const key = `${this.#submittedChoiceKeyPrefix}${scope}`
    const raw = this.getSafeItem(this.localStorageRef, key)
    if (!raw) return undefined
    try {
      const parsed = JSON.parse(raw) as PersistedSubmittedChoice
      if (this.isExpired(parsed.expiresAt)) {
        this.removeSafeItem(this.localStorageRef, key)
        return undefined
      }
      if (parsed.sessionId === scope && parsed.questionId === questionId) {
        return { answerIndex: parsed.answerIndex, label: parsed.label }
      }
      return undefined
    }
    catch {
      this.removeSafeItem(this.localStorageRef, key)
      return undefined
    }
  }

  clearSubmittedChoice(sessionId?: string): void {
    if (sessionId) {
      this.removeSafeItem(this.localStorageRef, `${this.#submittedChoiceKeyPrefix}${sessionId}`)
      return
    }
    this.removeKeysByPrefix(this.#submittedChoiceKeyPrefix)
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
    this.removeKeysByPrefix(this.#submittedChoiceKeyPrefix)
    this.removeKeysByPrefix(this.#finalResultsKeyPrefix)
    this.removeKeysByPrefix(this.#participantNameKeyPrefix)
    this.#participantNameSignal.set(null)
    this.#finalResultsSignal.set(null)
    const existing = this.retrieve()
    if (existing) {
      delete existing.participationToken
      this.setSafeItem(this.localStorageRef, this.#storageKey, JSON.stringify(existing))
    }
  }

  saveFinalResults(results: FinalParticipantResults, sessionId?: string): void {
    const scope = this.resolveSessionScope(sessionId)
    const envelope: PersistedFinalResultsEnvelope = {
      sessionId: scope,
      results,
      expiresAt: Date.now() + PERSISTENCE_TTL_MS,
    }
    this.setSafeItem(this.localStorageRef, `${this.#finalResultsKeyPrefix}${scope}`, JSON.stringify(envelope))
    this.#finalResultsSignal.set({ sessionId: scope, results })
  }

  getFinalResults(sessionId?: string): FinalParticipantResults | null {
    const sigVal = this.#finalResultsSignal()
    const scope = this.resolveSessionScope(sessionId)
    const key = `${this.#finalResultsKeyPrefix}${scope}`
    const raw = this.getSafeItem(this.localStorageRef, key)
    if (raw) {
      try {
        const parsed = JSON.parse(raw) as PersistedFinalResultsEnvelope
        if (this.isExpired(parsed.expiresAt)) {
          this.removeSafeItem(this.localStorageRef, key)
          if (sigVal?.sessionId === scope) {
            this.#finalResultsSignal.set(null)
          }
          return null
        }
        if (parsed.sessionId === scope && parsed.results) {
          return parsed.results
        }
      }
      catch {
        this.removeSafeItem(this.localStorageRef, key)
      }
    }
    return sigVal?.sessionId === scope ? sigVal.results : null
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
    this.removeKeysByPrefix(this.#submittedChoiceKeyPrefix)
    this.removeKeysByPrefix(this.#finalResultsKeyPrefix)
    this.removeKeysByPrefix(this.#participantNameKeyPrefix)
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

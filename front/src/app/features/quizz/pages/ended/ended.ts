import {
  ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { Router } from '@angular/router'

import { finalize } from 'rxjs'

import { isSuccess } from '@quezap/core/types'
import { Spinner } from '@quezap/shared/components/spinner/spinner'

import { JoinForm } from '../../components/join-form/join-form'
import {
  ActiveSessionPersistence,
  FinalParticipantResults,
} from '../../services/active-session-persistence/active-session-persistence'
import { SESSION_API_SERVICE } from '../../services/session-api/session-api'

@Component({
  selector: 'quizz-ended',
  imports: [
    JoinForm,
    Spinner,
  ],
  templateUrl: './ended.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Ended {
  private readonly router = inject(Router)
  private readonly persistence = inject(ActiveSessionPersistence)
  private readonly sessionApi = inject(SESSION_API_SERVICE, { optional: true })
  private readonly destroyRef = inject(DestroyRef)

  private readonly navigationState = this.router.currentNavigation()?.extras.state
    ?? (typeof history !== 'undefined' ? (history.state as Record<string, unknown> | undefined) : undefined)

  protected readonly isKicked = signal(Boolean(this.navigationState?.['kicked']))
  protected readonly kickedMessage = signal(
    typeof this.navigationState?.['message'] === 'string'
      ? this.navigationState['message']
      : 'Vous avez été exclu de la session',
  )

  private readonly fallbackResults = signal<FinalParticipantResults | null>(null)
  protected readonly isLoadingResults = signal(false)

  protected readonly nickname = computed(() =>
    this.persistence.getParticipantName() || this.persistence.retrieve()?.nickname || 'Joueur',
  )

  protected readonly finalResults = computed(() =>
    this.persistence.getFinalResults() ?? this.fallbackResults(),
  )

  protected readonly classAverageScore = computed(() =>
    this.finalResults()?.classAverageScore ?? 0,
  )

  protected readonly classSuccessRate = computed(() =>
    this.finalResults()?.classSuccessRate,
  )

  protected readonly fullLeaderboard = computed(() =>
    this.finalResults()?.fullLeaderboard ?? [],
  )

  protected readonly isAboveOrEqualAverage = computed(() => {
    const res = this.finalResults()
    if (!res || res.classAverageScore === undefined) return false
    return res.score >= res.classAverageScore
  })

  constructor() {
    if (!this.isKicked()) {
      this.loadFallbackScoresIfNeeded()
    }
  }

  private loadFallbackScoresIfNeeded(): void {
    if (this.persistence.getFinalResults() !== null) {
      return
    }

    const sessionId = this.persistence.getSessionId() ?? this.persistence.retrieve()?.sessionId
    if (!sessionId || !this.sessionApi?.getSessionScores) {
      return
    }

    this.isLoadingResults.set(true)
    this.sessionApi.getSessionScores(sessionId).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.isLoadingResults.set(false)),
    ).subscribe((output) => {
      if (!isSuccess(output)) {
        return
      }
      const entries = output.result.leaderboard ?? []
      if (entries.length === 0) {
        return
      }

      const myName = (this.persistence.getParticipantName(sessionId) || this.persistence.retrieve()?.nickname || '')
        .trim()
        .toLowerCase()
      const myEntry = myName
        ? entries.find(entry => (entry.participantName ?? '').trim().toLowerCase() === myName)
        : undefined
      const classAverage = Math.round(
        entries.reduce((acc, entry) => acc + (entry.score ?? 0), 0) / entries.length,
      )
      const rank = myEntry?.rank ?? (entries.length + 1)
      const results: FinalParticipantResults = {
        rank,
        totalParticipants: entries.length,
        score: myEntry?.score ?? 0,
        isPodium: myEntry?.rank !== undefined && myEntry.rank <= 3,
        classAverageScore: classAverage,
        fullLeaderboard: entries.map((entry, idx) => ({
          rank: entry.rank ?? (idx + 1),
          participantName: entry.participantName ?? 'Participant',
          score: entry.score ?? 0,
        })),
      }

      this.persistence.saveFinalResults(results, sessionId)
      this.fallbackResults.set(results)
    })
  }
}

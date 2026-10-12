import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'

import { JoinForm } from '../../components/join-form/join-form'
import { ActiveSessionPersistence } from '../../services/active-session-persistence/active-session-persistence'

@Component({
  selector: 'quizz-ended',
  imports: [
    JoinForm,
  ],
  templateUrl: './ended.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Ended {
  private readonly persistence = inject(ActiveSessionPersistence)

  protected readonly nickname = computed(() =>
    this.persistence.getParticipantName() || this.persistence.retrieve()?.nickname || 'Joueur',
  )

  protected readonly finalResults = computed(() =>
    this.persistence.getFinalResults(),
  )

  protected readonly classAverageScore = computed(() =>
    this.finalResults()?.classAverageScore ?? 0,
  )

  protected readonly classSuccessRate = computed(() =>
    this.finalResults()?.classSuccessRate ?? 0,
  )

  protected readonly fullLeaderboard = computed(() =>
    this.finalResults()?.fullLeaderboard ?? [],
  )

  protected readonly isAboveOrEqualAverage = computed(() => {
    const res = this.finalResults()
    if (!res || res.classAverageScore === undefined) return false
    return res.score >= res.classAverageScore
  })
}

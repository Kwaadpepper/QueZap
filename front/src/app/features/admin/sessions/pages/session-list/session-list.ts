import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { Router } from '@angular/router'

import { ButtonModule } from 'primeng/button'

import { IconFacade } from '@quezap/shared/components/icon/icon-facade'
import { Paginator } from '@quezap/shared/components/paginator/paginator'

import { SessionCard } from '../../components/session-card/session-card'
import { SessionPageStore } from '../../stores'

@Component({
  selector: 'quizz-session-list',
  imports: [Paginator, SessionCard, ButtonModule, IconFacade],
  templateUrl: './session-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SessionList {
  readonly #createPath = '/admin/sessions/create'
  private readonly router = inject(Router)

  // Session Pagination
  private readonly sessionPage = inject(SessionPageStore)
  protected readonly isLoading = computed(() => this.sessionPage.isLoading())
  protected readonly pageInfo = computed(() => this.sessionPage.pageInfo())
  protected readonly sessions = computed(() => this.sessionPage.pageData())

  protected nextPage() {
    this.sessionPage.setPagination({ page: this.sessionPage.pageInfo().page + 1 })
  }

  protected previousPage() {
    this.sessionPage.setPagination({ page: this.sessionPage.pageInfo().page - 1 })
  }

  protected setPageSize(size: number) {
    this.sessionPage.setPagination({ pageSize: size })
  }

  protected onPaginationChanged(pagination: { page: number, pageSize: number }) {
    this.sessionPage.setPagination(pagination)
  }

  protected onCreateSession() {
    this.router.navigate([this.#createPath])
  }
}

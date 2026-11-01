import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { Router } from '@angular/router'

import { ButtonModule } from 'primeng/button'

import { QuezapWithTheme } from '@quezap/domain/models'
import { Paginator } from '@quezap/shared/components/paginator/paginator'
import { AuthenticatedUserStore } from '@quezap/shared/stores/authenticated-user'

import { QuezapCard } from '../../components/quezap-card/quezap-card'
import { QuezapPageStore } from '../../stores'

@Component({
  selector: 'quizz-quezap-list',
  imports: [Paginator, QuezapCard, ButtonModule],
  templateUrl: './quezap-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuezapList {
  readonly #createPath = '/admin/quezaps/create'
  private readonly router = inject(Router)
  private readonly authStore = inject(AuthenticatedUserStore)

  // Quezap Pagination
  private readonly quezapPage = inject(QuezapPageStore)
  protected readonly isLoading = computed(() => this.quezapPage.isLoading())
  protected readonly pageInfo = computed(() => this.quezapPage.pageInfo())
  protected readonly quezaps = computed(() => this.quezapPage.pageData())

  protected isEditable(quezap: QuezapWithTheme): boolean {
    const currentUserId = this.authStore.currentUser()?.id
    if (!quezap.authorId || !currentUserId) {
      return true
    }
    return quezap.authorId === currentUserId
  }

  protected nextPage() {
    this.quezapPage.setPagination({ page: this.quezapPage.pageInfo().page + 1 })
  }

  protected previousPage() {
    this.quezapPage.setPagination({ page: this.quezapPage.pageInfo().page - 1 })
  }

  protected setPageSize(size: number) {
    this.quezapPage.setPagination({ pageSize: size })
  }

  protected onPaginationChanged(pagination: { page: number, pageSize: number }) {
    this.quezapPage.setPagination(pagination)
  }

  protected onCreateQuezap() {
    this.router.navigate([this.#createPath])
  }
}

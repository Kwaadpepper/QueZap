import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { Router } from '@angular/router'

import { ButtonDirective } from 'primeng/button'

import { Paginator } from '@quezap/shared/components'

import { QuezapCard } from '../../components'
import { QuezapPageStore } from '../../stores'

@Component({
  selector: 'quizz-quezap-list',
  imports: [Paginator, QuezapCard, ButtonDirective],
  templateUrl: './quezap-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuezapList {
  readonly #createPath = '/admin/quezaps/create'
  private readonly router = inject(Router)

  // Quezap Pagination
  private readonly quezapPage = inject(QuezapPageStore)
  protected readonly isLoading = computed(() => this.quezapPage.isLoading())
  protected readonly pageInfo = computed(() => this.quezapPage.pageInfo())
  protected readonly quezaps = computed(() => this.quezapPage.pageData())

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

  protected onCreateTheme() {
    this.router.navigate([this.#createPath])
  }
}

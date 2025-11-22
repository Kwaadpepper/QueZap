import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'

import { Paginator } from '@quezap/shared/components'

import { QuezapCard } from '../../components'
import { QuezapPageStore } from '../../stores'

@Component({
  selector: 'quizz-quezap-list',
  imports: [Paginator, QuezapCard],
  templateUrl: './quezap-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuezapList {
  // Quezap Pagination
  protected readonly quezapPage = inject(QuezapPageStore)

  protected readonly isLoading = computed(() => this.quezapPage.isLoading())
  protected readonly pageInfo = computed(() => this.quezapPage.pageInfo())
  protected readonly quezaps = computed(() => this.quezapPage.pageData())

  nextPage() {
    this.quezapPage.setPagination({ page: this.quezapPage.pageInfo().page + 1 })
  }

  previousPage() {
    this.quezapPage.setPagination({ page: this.quezapPage.pageInfo().page - 1 })
  }

  setPageSize(size: number) {
    this.quezapPage.setPagination({ pageSize: size })
  }

  onPaginationChanged(pagination: { page: number, pageSize: number }) {
    this.quezapPage.setPagination(pagination)
  }
}

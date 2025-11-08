import { Component, computed, inject } from '@angular/core'

import { Paginator } from '@quezap/shared/components/paginator/paginator'

import { ThemeCard } from '../components'
import { ThemePageStore } from '../stores'

@Component({
  selector: 'quizz-theme-list',
  imports: [
    ThemeCard,
    Paginator,
  ],
  templateUrl: './theme-list.html',
  styleUrl: './theme-list.css',
})
export class ThemeList {
  protected readonly themePage = inject(ThemePageStore)

  public readonly isLoading = computed(() => this.themePage.isLoading())
  public readonly pageInfo = computed(() => this.themePage.pageInfo())
  public readonly themes = computed(() => this.themePage.pageData())

  nextPage() {
    this.themePage.setPagination({ page: this.themePage.pageInfo().page + 1 })
  }

  previousPage() {
    this.themePage.setPagination({ page: this.themePage.pageInfo().page - 1 })
  }

  setPageSize(size: number) {
    this.themePage.setPagination({ pageSize: size })
  }

  onPaginationChanged(pagination: { page: number, pageSize: number }) {
    console.debug('Pagination changed:', pagination)
    this.themePage.setPagination(pagination)
  }
}

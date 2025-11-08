import { Component, computed, input, output } from '@angular/core'

import { Paginator as PaginatorComponent, PaginatorState } from 'primeng/paginator'

export interface PaginatorPageInfo {
  page: number
  pageSize: number
  totalElements: number
  totalPages: number
}

export interface PaginatorEvent {
  page: number
  pageSize: number
}

@Component({
  selector: 'quizz-paginator',
  imports: [PaginatorComponent],
  templateUrl: './paginator.html',
})
export class Paginator {
  protected readonly defaultPerPage = 10
  protected readonly perPageOptions = [2, 5, this.defaultPerPage, 25, 35, 50]

  public readonly pageInfo = input.required<PaginatorPageInfo>()
  public readonly pageSize = input<number>(this.defaultPerPage)

  public readonly paginationChanged = output<PaginatorEvent>()

  // Adaptative display properties
  protected readonly showFirstLastIcon = computed(() => this.pageInfo().totalPages > 5)
  protected readonly showJumpToPageDropdown = computed(() => this.pageInfo().totalPages > 10)
  protected readonly showPageLinks = computed(() => this.pageInfo().totalPages <= 20)
  protected readonly showCurrentPageReport = computed(() => true)

  protected onPageChange(event: PaginatorState): void {
    const page = (event.page ?? 0) + 1
    const pageSize = event.rows ?? this.defaultPerPage

    console.debug('Page changed:', { page, pageSize })

    this.paginationChanged.emit({ page, pageSize })
  }
}

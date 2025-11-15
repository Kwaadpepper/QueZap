import { Component, computed, inject, signal } from '@angular/core'

import { ButtonDirective, ButtonIcon } from 'primeng/button'

import { Paginator } from '@quezap/shared/components/paginator/paginator'

import { ThemeCard, ThemeEditor } from '../../components'
import { ThemePageStore } from '../../stores'

@Component({
  selector: 'quizz-theme-list',
  imports: [
    ThemeCard,
    Paginator,
    ButtonDirective,
    ButtonIcon,
    ThemeEditor,
  ],
  templateUrl: './theme-list.html',
  styleUrl: './theme-list.css',
})
export class ThemeList {
  // Theme Pagination
  protected readonly themePage = inject(ThemePageStore)

  protected readonly isLoading = computed(() => this.themePage.isLoading())
  protected readonly pageInfo = computed(() => this.themePage.pageInfo())
  protected readonly themes = computed(() => this.themePage.pageData())

  // Theme Editor
  protected isThemeEditorVisible = signal(false)

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
    this.themePage.setPagination(pagination)
  }

  onCreateTheme() {
    this.isThemeEditorVisible.set(true)
  }

  onThemeSaved() {
    this.themePage.reload()
  }
}

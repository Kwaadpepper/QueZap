import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'

import { ButtonModule } from 'primeng/button'

import { IconFacade } from '@quezap/shared/components/icon/icon-facade'
import { Paginator } from '@quezap/shared/components/paginator/paginator'

import { ThemeCard } from '../../components/theme-card/theme-card'
import { ThemeEditor } from '../../components/theme-editor/theme-editor'
import { ThemesEventsBus } from '../../services'
import { ThemePageStore } from '../../stores'

@Component({
  selector: 'quizz-theme-list',
  imports: [
    ThemeCard,
    Paginator,
    ButtonModule,
    ThemeEditor,
    IconFacade,
  ],
  templateUrl: './theme-list.html',
  styleUrl: './theme-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ThemeList {
  // Theme Pagination
  protected readonly themePage = inject(ThemePageStore)

  protected readonly isLoading = computed(() => this.themePage.isLoading())
  protected readonly pageInfo = computed(() => this.themePage.pageInfo())
  protected readonly themes = computed(() => this.themePage.pageData())

  // Theme Editor

  private readonly themesEventBus = inject(ThemesEventsBus)
  protected isThemeEditorVisible = signal(false)

  constructor() {
    this.themesEventBus.themeChanged.pipe(
      takeUntilDestroyed(),
    ).subscribe(() => {
      console.log('Theme changed event received in ThemeList')
      this.themePage.reload()
    })
  }

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
}

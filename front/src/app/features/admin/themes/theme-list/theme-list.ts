import { Component, computed, inject } from '@angular/core'

import { ButtonModule } from 'primeng/button'

import { ThemeCard } from '../components'
import { ThemePageStore } from '../stores'

@Component({
  selector: 'quizz-theme-list',
  imports: [
    ButtonModule,
    ThemeCard,
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
    this.themePage.setPage(this.themePage.pageInfo().page + 1)
  }

  previousPage() {
    this.themePage.setPage(this.themePage.pageInfo().page - 1)
  }

  setPageSize(pageSize: number) {
    this.themePage.setPageSize(pageSize)
  }
}

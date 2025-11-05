import { Routes } from '@angular/router'

export const routes: Routes = [
  {
    path: '',
    loadChildren: () =>
      import('./theme-list/theme-list').then(
        m => m.ThemeList,
      ),
  },
]

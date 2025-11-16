import { Routes } from '@angular/router'

import { THEME_SERVICE, ThemeMockService } from './themes/services'
import { ThemePageStore } from './themes/stores'

export const routes: Routes = [
  {
    path: 'dashboard',
    title: 'Tableau de bord',
    loadChildren: () => import('./dashboard/routes').then(m => m.routes),
  },
  {
    path: 'themes',
    title: 'Thèmes',
    providers: [
      ThemePageStore,
      { provide: THEME_SERVICE, useClass: ThemeMockService },
    ],
    loadChildren: () => import('./themes/routes').then(m => m.routes),
  },
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
]

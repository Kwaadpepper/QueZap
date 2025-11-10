import { Routes } from '@angular/router'

import { routes as dashBoardRoutes } from './dashboard/routes'
import { routes as themesRoutes } from './themes/routes'

export const routes: Routes = [
  {
    path: 'dashboard',
    title: 'Tableau de bord',
    children: dashBoardRoutes,
  },
  {
    path: 'themes',
    title: 'Thèmes',
    children: themesRoutes,
  },
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
]

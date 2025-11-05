import { Routes } from '@angular/router'

import { routes as dashBoardRoutes } from './dashboard/routes'
import { routes as themesRoutes } from './themes/routes'

export const routes: Routes = [
  {
    path: 'dashboard',
    children: dashBoardRoutes,
  },
  {
    path: 'themes',
    children: themesRoutes,
  },
]

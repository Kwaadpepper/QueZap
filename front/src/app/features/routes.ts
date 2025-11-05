import { Routes } from '@angular/router'

import { routes as AdminRoutes } from './admin/routes'

export const routes: Routes = [
  {
    path: 'admin',
    children: AdminRoutes,
  },
]

import { Routes } from '@angular/router'

import { routes as ActivationRoutes } from './activation/routes'
import { routes as AdminRoutes } from './admin/routes'
import { routes as PagesRoutes } from './pages/routes'

export const routes: Routes = [
  {
    path: 'admin',
    title: 'Administration',
    children: AdminRoutes,
  },
  ...ActivationRoutes,
  ...PagesRoutes,
]

import { Routes } from '@angular/router'

import { routes as FeatureRoutes } from './features/routes'
import { notFoundRoute, routes as PagesRoutes } from './pages/routes'

export const routes: Routes = [
  ...PagesRoutes,
  ...FeatureRoutes,
  notFoundRoute,
]

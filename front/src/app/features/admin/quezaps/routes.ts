import { Routes } from '@angular/router'

import { quezapResolver } from './resolvers'

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages').then(m => m.QuezapList),
  },
  {
    path: 'create',
    loadComponent: () => import('./pages').then(m => m.QuezapCreate),
  },
  {
    path: ':quezap/edit',
    resolve: { quezap: quezapResolver },
    loadComponent: () => import('./pages').then(m => m.QuezapEdit),
  },
]

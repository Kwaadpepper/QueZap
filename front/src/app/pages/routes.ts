import { Routes } from '@angular/router'

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./home/home').then(m => m.Home),
  },
  {
    path: 'template',
    title: 'Deboggeur de template',
    loadComponent: () => import('./template/template').then(m => m.Template),
  },
  {
    path: 'licence',
    title: 'Licence',
    loadComponent: () => import('./licence/licence').then(m => m.Licence),
  },
  {
    path: 'about',
    title: 'À propos',
    loadComponent: () => import('./about/about').then(m => m.About),
  },
]

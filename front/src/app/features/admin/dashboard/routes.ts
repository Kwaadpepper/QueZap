import { Routes } from '@angular/router'

export const routes: Routes = [
  {
    path: '',
    loadChildren: () =>
      import('./home/home').then(
        m => m.Home,
      ),
  },
]

import { Routes } from '@angular/router'

import { ACCOUNT_ACTIVATION_SERVICE, AccountActivationMockService } from './services'

export const routes: Routes = [
  {
    path: '',
    providers: [
      { provide: ACCOUNT_ACTIVATION_SERVICE, useClass: AccountActivationMockService },
    ],
    loadComponent: () => import('./account-activation/account-activation').then(m => m.AccountActivation),
  },
]

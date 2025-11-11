import { InjectionToken } from '@angular/core'

import { Observable } from 'rxjs'

export interface AccountActivationService {
  activate(token: string): Observable<void>
}

export const ACCOUNT_ACTIVATION_SERVICE = new InjectionToken<AccountActivationService>('AccountActivationService')

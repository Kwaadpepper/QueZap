import { InjectionToken } from '@angular/core'

import { ServiceOutput } from '@quezap/core/types'

export interface AccountActivationService {
  activate(token: string): ServiceOutput<void>
}

export const ACCOUNT_ACTIVATION_SERVICE = new InjectionToken<AccountActivationService>('AccountActivationService')

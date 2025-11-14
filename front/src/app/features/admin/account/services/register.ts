import { InjectionToken } from '@angular/core'

import { ServiceOutput } from '@quezap/core/types'

export interface RegisterService {
  register(email: string): ServiceOutput<void>
}

export const REGISTER_SERVICE = new InjectionToken<RegisterService>('RegisterService')

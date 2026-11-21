import { InjectionToken } from '@angular/core'

import { ServiceOutput } from '@quezap/core/types'

export interface RegisterService {
  register(email: string, password: string, turnstileToken?: string): ServiceOutput<void>
}

export const REGISTER_SERVICE = new InjectionToken<RegisterService>('RegisterService')

import { InjectionToken } from '@angular/core'

import { Observable } from 'rxjs'

export interface RegisterService {
  register(email: string, username: string): Observable<void>
}

export const REGISTER_SERVICE = new InjectionToken<RegisterService>('RegisterService')

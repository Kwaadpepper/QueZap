import { InjectionToken } from '@angular/core'

import { PageOf, Pagination, ServiceOutput } from '@quezap/core/types'
import { Quezap } from '@quezap/domain/models'

export interface QuezapService {
  getQuezapPage(page: Pagination): ServiceOutput<PageOf<Quezap>>
}

export const QUEZAP_SERVICE = new InjectionToken<QuezapService>('QuezapService')

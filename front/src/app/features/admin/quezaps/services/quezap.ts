import { InjectionToken } from '@angular/core'

import { PageOf, Pagination, ServiceOutput } from '@quezap/core/types'
import { Quezap, QuezapWithQuestionsAndAnswers } from '@quezap/domain/models'

export interface QuezapService {
  getQuezapPage(page: Pagination): ServiceOutput<PageOf<Quezap>>

  persistQuezap(quezap: Omit<QuezapWithQuestionsAndAnswers, 'id'>): ServiceOutput<Pick<Quezap, 'id'>>
}

export const QUEZAP_SERVICE = new InjectionToken<QuezapService>('QuezapService')

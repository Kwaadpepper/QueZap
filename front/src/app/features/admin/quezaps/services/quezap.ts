import { InjectionToken } from '@angular/core'

import { NotFoundError } from '@quezap/core/errors'
import { PageOf, Pagination, ServiceOutput } from '@quezap/core/types'
import { Quezap, QuezapId, QuezapWithQuestionsAndAnswers, QuezapWithTheme } from '@quezap/domain/models'

export interface QuezapService {
  getQuezapPage(page: Pagination): ServiceOutput<PageOf<QuezapWithTheme>>

  find(quezapId: QuezapId): ServiceOutput<QuezapWithQuestionsAndAnswers, NotFoundError>

  persistQuezap(
    quezap: QuezapWithQuestionsAndAnswers | (Omit<QuezapWithQuestionsAndAnswers, 'id'> & { id?: QuezapId }),
  ): ServiceOutput<Pick<Quezap, 'id'>>

  duplicateQuezap(quezapId: QuezapId): ServiceOutput<Pick<Quezap, 'id'>>

  deleteQuezap(quezapId: QuezapId): ServiceOutput<void>
}

export const QUEZAP_SERVICE = new InjectionToken<QuezapService>('QuezapService')

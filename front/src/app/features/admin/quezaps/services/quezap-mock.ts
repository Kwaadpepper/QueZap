import { Injectable, signal } from '@angular/core'

import { delay, map, of, tap } from 'rxjs'

import { ServiceError } from '@quezap/core/errors'
import { PageOf, Pagination, ServiceOutput, toPageBasedPagination } from '@quezap/core/types'
import { Quezap, QuezapWithQuestionsAndAnswers } from '@quezap/domain/models'

import { QuezapService } from './quezap'
import { MOCK_QUEZAPS } from './quezap.mock'

@Injectable()
export class QuezapMockService implements QuezapService {
  private readonly MOCK_DELAY = () => Math.max(100, Math.random() * 3000)
  private readonly MOCK_ERROR = () => Math.random() < 0.2

  private readonly mockedQuezaps = signal<Quezap[]>(MOCK_QUEZAPS)

  getQuezapPage(page: Pagination): ServiceOutput<PageOf<Quezap>> {
    const pagination = toPageBasedPagination(page)
    const quezaps: Quezap[] = this.mockedQuezaps().map(quezap => ({
      id: quezap.id,
      title: quezap.title,
      description: quezap.description,
    }))

    const startIndex = (pagination.page - 1) * pagination.pageSize
    const endIndex = startIndex + pagination.pageSize
    const pagedQuezaps = quezaps.slice(startIndex, endIndex)

    const pageOfQuezaps: PageOf<Quezap> = {
      data: pagedQuezaps,
      totalElements: quezaps.length,
      totalPages: Math.ceil(quezaps.length / pagination.pageSize),
      page: pagination.page,
      pageSize: pagination.pageSize,
      hasNext: endIndex < quezaps.length,
      hasPrevious: startIndex > 0,
    }

    return of(pageOfQuezaps).pipe(
      delay(this.MOCK_DELAY()),
      tap(() => {
        if (this.MOCK_ERROR()) {
          console.debug('Mock: error while fetching quezaps')
          throw new ServiceError('Mock service error: quezaps')
        }
      }),
      map((obs) => {
        console.debug('Mock: fetched quezaps page', pageOfQuezaps)
        return {
          kind: 'success',
          result: obs,
        }
      }),
    )
  }

  persistQuezap(quezap: Omit<QuezapWithQuestionsAndAnswers, 'id'>): ServiceOutput<Pick<Quezap, 'id'>> {
    return of(quezap).pipe(
      delay(this.MOCK_DELAY()),
      tap(() => {
        if (this.MOCK_ERROR()) {
          console.debug('Mock: error while persisting quezap')
          throw new ServiceError('Mock service error: quezap persist')
        }
      }),
      map(() => {
        console.debug('Mock: persisted quezap')
        return {
          kind: 'success',
          result: this.mockedQuezaps()[0].id,
        }
      }),
    )
  }
}

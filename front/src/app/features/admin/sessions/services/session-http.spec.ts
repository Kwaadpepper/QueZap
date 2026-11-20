import { TestBed } from '@angular/core/testing'

import { firstValueFrom } from 'rxjs'

import * as sdk from '@quezap/core/api-client/sdk.gen'
import { isFailure, isSuccess } from '@quezap/core/types'

import { SessionHttpService } from './session-http'

describe('SessionHttpService', () => {
  let service: SessionHttpService

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [SessionHttpService] })
    service = TestBed.inject(SessionHttpService)
  })

  afterEach(() => {
    jest.restoreAllMocks()
  })

  it('should call sdk list1 with page and perPage using form explode querySerializer', async () => {
    const spy = jest.spyOn(sdk, 'list1').mockResolvedValueOnce({
      data: {
        data: [{ id: 'sess-1', name: 'Maths 101', code: 'QZ1234', status: 'LOBBY' }],
        totalElements: 1,
        totalPages: 1,
        page: 2,
        pageSize: 20,
        hasNext: false,
        hasPrevious: true,
      },
      error: undefined,
    } as never)

    const result = await firstValueFrom(service.getSessionPage({ page: 2, pageSize: 20 }))

    expect(spy).toHaveBeenCalledWith({
      query: { paginationDto: { page: 2, perPage: 20 } },
      querySerializer: { object: { style: 'form', explode: true } },
    })
    expect(isSuccess(result)).toBe(true)
    if (isSuccess(result)) {
      expect(result.result.data[0].name).toBe('Maths 101')
      expect(result.result.page).toBe(2)
      expect(result.result.pageSize).toBe(20)
    }
  })

  it('should return failure when list1 rejects', async () => {
    jest.spyOn(sdk, 'list1').mockRejectedValueOnce(new Error('Network error'))

    const result = await firstValueFrom(service.getSessionPage({ page: 1, pageSize: 10 }))

    expect(isFailure(result)).toBe(true)
  })

  it('should create session and return SessionCreatedDto', async () => {
    const spy = jest.spyOn(sdk, 'create1').mockResolvedValueOnce({
      data: { id: 'sess-new', number: 42, code: 'QZ0042' },
      error: undefined,
    } as never)

    const result = await firstValueFrom(service.createSession('quiz-1', 'Session Test'))

    expect(spy).toHaveBeenCalledWith({ body: { quezapId: 'quiz-1', name: 'Session Test' } })
    expect(isSuccess(result)).toBe(true)
    if (isSuccess(result)) {
      expect(result.result).toEqual({ id: 'sess-new', number: 42, code: 'QZ0042' })
    }
  })

  it('should export CSV as Blob via sdk exportCsv', async () => {
    const csvBlob = new Blob(['rank,name,score\n1,Alice,1000\n'], { type: 'text/csv' })
    const spy = jest.spyOn(sdk, 'exportCsv').mockResolvedValueOnce({
      data: csvBlob,
      error: undefined,
    } as never)

    const result = await firstValueFrom(service.exportCsv('sess-1'))

    expect(spy).toHaveBeenCalledWith({
      path: { id: 'sess-1' },
      responseType: 'blob',
    })
    expect(isSuccess(result)).toBe(true)
    if (isSuccess(result)) {
      expect(result.result).toBe(csvBlob)
    }
  })

  it('should return failure when exportCsv rejects', async () => {
    jest.spyOn(sdk, 'exportCsv').mockRejectedValueOnce(new Error('Export failed'))

    const result = await firstValueFrom(service.exportCsv('sess-1'))

    expect(isFailure(result)).toBe(true)
  })
})

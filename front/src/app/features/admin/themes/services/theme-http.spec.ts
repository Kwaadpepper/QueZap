import { TestBed } from '@angular/core/testing'

import { firstValueFrom } from 'rxjs'

import * as sdk from '@quezap/core/api-client/sdk.gen'
import { isFailure, isSuccess } from '@quezap/core/types'
import { ThemeId } from '@quezap/domain/models'

import { ThemeHttpService } from './theme-http'

describe('ThemeHttpService', () => {
  let service: ThemeHttpService

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [ThemeHttpService] })
    service = TestBed.inject(ThemeHttpService)
  })

  afterEach(() => {
    jest.restoreAllMocks()
  })

  it('should call sdk find with page and perPage using form explode querySerializer', async () => {
    const spy = jest.spyOn(sdk, 'find').mockResolvedValueOnce({
      data: {
        data: [{ id: 'theme-1', name: 'Sciences' }],
        totalElements: 1,
        totalPages: 1,
        page: 2,
        pageSize: 15,
        hasNext: false,
        hasPrevious: true,
      },
      error: undefined,
    } as never)

    const result = await firstValueFrom(service.getThemePage({ page: 2, pageSize: 15 }))

    expect(spy).toHaveBeenCalledWith({
      query: { paginationDto: { page: 2, perPage: 15 }, queryDto: {} },
      querySerializer: { object: { style: 'form', explode: true } },
    })
    expect(isSuccess(result)).toBe(true)
    if (isSuccess(result)) {
      expect(result.result.data).toEqual([{ id: 'theme-1' as ThemeId, name: 'Sciences' }])
      expect(result.result.page).toBe(2)
      expect(result.result.pageSize).toBe(15)
    }
  })

  it('should return failure when find rejects', async () => {
    jest.spyOn(sdk, 'find').mockRejectedValueOnce(new Error('Network error'))

    const result = await firstValueFrom(service.getThemePage({ page: 1, pageSize: 10 }))

    expect(isFailure(result)).toBe(true)
  })

  it('should create, update, and delete themes via sdk functions', async () => {
    const addSpy = jest.spyOn(sdk, 'add').mockResolvedValueOnce({
      data: { id: 'theme-new' },
      error: undefined,
    } as never)
    const renameSpy = jest.spyOn(sdk, 'rename1').mockResolvedValueOnce({ error: undefined } as never)
    const deleteSpy = jest.spyOn(sdk, 'delete1').mockResolvedValueOnce({ error: undefined } as never)

    const created = await firstValueFrom(service.create({ name: 'Histoire' }))
    expect(isSuccess(created)).toBe(true)
    expect(addSpy).toHaveBeenCalledWith({ body: { name: 'Histoire' } })

    const updated = await firstValueFrom(service.update({ id: 'theme-new' as ThemeId, name: 'Géo' }))
    expect(isSuccess(updated)).toBe(true)
    expect(renameSpy).toHaveBeenCalledWith({ path: { id: 'theme-new' }, body: { name: 'Géo' } })

    const deleted = await firstValueFrom(service.delete('theme-new' as ThemeId))
    expect(isSuccess(deleted)).toBe(true)
    expect(deleteSpy).toHaveBeenCalledWith({ path: { id: 'theme-new' } })
  })

  it('should cap perPage at 50 when pageSize exceeds 50', async () => {
    const findSpy = jest.spyOn(sdk, 'find').mockResolvedValueOnce({
      data: {
        data: [],
        totalElements: 0,
        totalPages: 0,
        page: 1,
        pageSize: 50,
        hasNext: false,
        hasPrevious: false,
      },
      error: undefined,
    } as never)

    await firstValueFrom(service.getThemePage({ page: 1, pageSize: 100 }))

    expect(findSpy).toHaveBeenCalledWith(
      expect.objectContaining({ query: { paginationDto: { page: 1, perPage: 50 }, queryDto: {} } }),
    )
  })
})

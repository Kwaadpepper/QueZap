import { TestBed } from '@angular/core/testing'

import { of } from 'rxjs'

import * as sdk from '@quezap/core/api-client/sdk.gen'
import { isSuccess } from '@quezap/core/types'
import { QuestionId, QuestionType, QuezapId } from '@quezap/domain/models'

import { QuezapHttpService } from './quezap-http'

describe('QuezapHttpService', () => {
  let service: QuezapHttpService

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [QuezapHttpService] })
    service = TestBed.inject(QuezapHttpService)
  })

  afterEach(() => {
    jest.restoreAllMocks()
  })

  it('should send perPage (and not pageSize) via sdk.list2 in getQuezapPage', (done) => {
    const listSpy = jest.spyOn(sdk, 'list2').mockReturnValue(
      of({
        data: {
          data: [],
          totalElements: 0,
          totalPages: 0,
          page: 2,
          pageSize: 10,
          hasNext: false,
          hasPrevious: true,
        },
        error: undefined,
      }) as never,
    )

    service.getQuezapPage({ page: 2, pageSize: 10 }).subscribe((output) => {
      expect(isSuccess(output)).toBe(true)
      expect(listSpy).toHaveBeenCalledTimes(1)
      const callArg = listSpy.mock.calls[0][0]
      expect(callArg.query.paginationDto).toEqual({ page: 2, perPage: 10 })
      const serialized = callArg.querySerializer?.(callArg.query)
      expect(serialized).toContain('perPage=10')
      expect(serialized).not.toContain('pageSize')
      done()
    })
  })

  it('should preserve points: 0, isPublic and themeId in find()', (done) => {
    jest.spyOn(sdk, 'getById').mockReturnValue(
      of({
        data: {
          id: 'q1',
          title: 'Quiz 0 pt',
          description: '',
          isPublic: false,
          slides: [
            {
              order: 1,
              timer: 20,
              points: 0,
              question: {
                id: 'qu1',
                type: 'QUIZZ',
                label: 'Sondage',
                theme: { id: 'theme-1', name: 'Science' },
                answers: [{ id: 'a1', label: 'Oui', isCorrect: true }],
              },
            },
          ],
        },
        error: undefined,
      }) as never,
    )

    service.find('q1' as QuezapId).subscribe((output) => {
      expect(isSuccess(output)).toBe(true)
      if (isSuccess(output)) {
        const result = output.result as typeof output.result & { isPublic?: boolean }
        expect(result.isPublic).toBe(false)
        expect(result.questionWithAnswersAndResponses[0].answers[0].points).toBe(0)
        expect((result.questionWithAnswersAndResponses[0] as { themeId?: string }).themeId).toBe('theme-1')
      }
      done()
    })
  })

  it('should preserve points: 0, isPublic and themeId in persistQuezap()', (done) => {
    const updateSpy = jest.spyOn(sdk, 'update').mockReturnValue(
      of({ data: undefined, error: undefined }) as never,
    )

    service.persistQuezap({
      id: 'q1' as QuezapId,
      title: 'Quiz 0 pt',
      description: '',
      isPublic: false,
      questionWithAnswersAndResponses: [
        {
          id: 'qu1' as QuestionId,
          value: 'Sondage',
          type: QuestionType.Quizz,
          limit: { seconds: 20 },
          themeId: 'theme-1',
          answers: [{ index: 0, points: 0, value: 'Oui', isCorrect: true }],
        } as never,
      ],
    } as never).subscribe((output) => {
      expect(isSuccess(output)).toBe(true)
      expect(updateSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          path: { id: 'q1' },
          body: expect.objectContaining({
            isPublic: false,
            slides: [
              expect.objectContaining({
                points: 0,
                question: expect.objectContaining({ themeId: 'theme-1' }),
              }),
            ],
          }),
        }),
      )
      done()
    })
  })
})

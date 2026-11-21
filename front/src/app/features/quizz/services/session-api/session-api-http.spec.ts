import { TestBed } from '@angular/core/testing'

import { firstValueFrom } from 'rxjs'

import * as sdk from '@quezap/core/api-client/sdk.gen'
import { ExpiredError, ForbidenError, NotFoundError, UnauthorizedError, ValidationError } from '@quezap/core/errors'
import { isFailure, isSuccess } from '@quezap/core/types'
import { SessionCode } from '@quezap/domain/models'

import { ActiveSessionPersistence } from '../active-session-persistence/active-session-persistence'

import { SessionApiHttpService } from './session-api-http'

describe('SessionApiHttpService', () => {
  let service: SessionApiHttpService
  let persistence: ActiveSessionPersistence

  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    TestBed.configureTestingModule({ providers: [SessionApiHttpService, ActiveSessionPersistence] })
    service = TestBed.inject(SessionApiHttpService)
    persistence = TestBed.inject(ActiveSessionPersistence)
  })

  afterEach(() => {
    jest.restoreAllMocks()
    localStorage.clear()
    sessionStorage.clear()
  })

  describe('joinSessionWith', () => {
    it('should return session on 200 success via sdk.getByCode', async () => {
      const spy = jest.spyOn(sdk, 'getByCode').mockResolvedValue({
        data: {
          id: '018e3a2b-8888-7def-8123-000000000001',
          code: '20082',
          name: 'Culture Générale',
          status: 'STARTED',
          startedAt: '2026-11-18T10:00:00Z',
        },
        error: undefined,
        request: {} as never,
        response: { status: 200 } as never,
      })

      const result = await firstValueFrom(service.joinSessionWith(' 200 82 ' as SessionCode))

      expect(spy).toHaveBeenCalledWith(expect.objectContaining({ path: { code: '20082' } }))
      expect(isSuccess(result)).toBe(true)
      if (isSuccess(result)) {
        expect(result.result.id).toBe('018e3a2b-8888-7def-8123-000000000001')
        expect(result.result.status).toBe('STARTED')
        expect(result.result.endedAt).toBeNull()
      }
    })

    it('should return NotFoundError on 404', async () => {
      jest.spyOn(sdk, 'getByCode').mockResolvedValue({
        data: undefined,
        error: { message: 'Not found' },
        request: {} as never,
        response: { status: 404 } as never,
      })

      const result = await firstValueFrom(service.joinSessionWith('99999' as SessionCode))
      expect(isFailure(result)).toBe(true)
      if (isFailure(result)) {
        expect(result.error).toBeInstanceOf(NotFoundError)
      }
    })

    it('should return ExpiredError when session is ENDED for non-participant', async () => {
      jest.spyOn(sdk, 'getByCode').mockResolvedValue({
        data: {
          id: '018e3a2b-8888-7def-8123-000000000001',
          code: '20082',
          name: 'Terminé',
          status: 'ENDED',
          endedAt: '2026-11-18T11:00:00Z',
        },
        error: undefined,
        request: {} as never,
        response: { status: 200 } as never,
      })

      const result = await firstValueFrom(service.joinSessionWith('20082' as SessionCode))
      expect(isFailure(result)).toBe(true)
      if (isFailure(result)) {
        expect(result.error).toBeInstanceOf(ExpiredError)
      }
    })
  })

  describe('submitAnswer', () => {
    beforeEach(() => {
      persistence.saveParticipationData('018e3a2b-8888-7def-8123-000000000001', 'ptk_123', 'Léa')
    })

    it('should succeed on HTTP 201 via sdk.submitAnswer', async () => {
      const spy = jest.spyOn(sdk, 'submitAnswer').mockResolvedValue({
        data: { accepted: true },
        error: undefined,
        request: {} as never,
        response: { status: 201 } as never,
      })

      const result = await firstValueFrom(service.submitAnswer(2))

      expect(spy).toHaveBeenCalledWith(expect.objectContaining({
        path: { id: '018e3a2b-8888-7def-8123-000000000001' },
        body: { answerIndex: 2 },
        headers: { 'X-Participant-Token': 'ptk_123' },
      }))
      expect(isSuccess(result)).toBe(true)
    })

    it('should return clear ValidationError on HTTP 409 (late answer)', async () => {
      jest.spyOn(sdk, 'submitAnswer').mockResolvedValue({
        data: undefined,
        error: { message: 'Les réponses sont verrouillées pour cette question' },
        request: {} as never,
        response: { status: 409 } as never,
      })

      const result = await firstValueFrom(service.submitAnswer(1))
      expect(isFailure(result)).toBe(true)
      if (isFailure(result)) {
        expect(result.error).toBeInstanceOf(ValidationError)
        expect(result.error.message).toContain('verrouillées')
      }
    })

    it('should return clear ValidationError on HTTP 400 (time expired)', async () => {
      jest.spyOn(sdk, 'submitAnswer').mockResolvedValue({
        data: undefined,
        error: { message: 'Le temps imparti est écoulé' },
        request: {} as never,
        response: { status: 400 } as never,
      })

      const result = await firstValueFrom(service.submitAnswer(0))
      expect(isFailure(result)).toBe(true)
      if (isFailure(result)) {
        expect(result.error).toBeInstanceOf(ValidationError)
        expect(result.error.message).toContain('écoulé')
      }
    })

    it('should return ForbidenError on HTTP 403 (kicked)', async () => {
      jest.spyOn(sdk, 'submitAnswer').mockResolvedValue({
        data: undefined,
        error: { message: 'Participant was kicked from the session' },
        request: {} as never,
        response: { status: 403 } as never,
      })

      const result = await firstValueFrom(service.submitAnswer(0))
      expect(isFailure(result)).toBe(true)
      if (isFailure(result)) {
        expect(result.error).toBeInstanceOf(ForbidenError)
        expect(result.error.message).toContain('exclu')
      }
    })

    it('should return ServiceError when offline (status 0)', async () => {
      jest.spyOn(sdk, 'submitAnswer').mockResolvedValue({
        data: undefined,
        error: new ProgressEvent('error'),
        request: {} as never,
        response: { status: 0 } as never,
      })

      const result = await firstValueFrom(service.submitAnswer(0))
      expect(isFailure(result)).toBe(true)
      if (isFailure(result)) {
        expect(result.error.message).toContain('Connexion perdue')
      }
    })

    it('should fail immediately with UnauthorizedError when token is missing', async () => {
      persistence.clear()
      const spy = jest.spyOn(sdk, 'submitAnswer')

      const result = await firstValueFrom(service.submitAnswer(0))
      expect(spy).not.toHaveBeenCalled()
      expect(isFailure(result)).toBe(true)
      if (isFailure(result)) {
        expect(result.error).toBeInstanceOf(UnauthorizedError)
      }
    })
  })

  describe('getSessionScores', () => {
    it('should return validated LeaderboardResponseDto and include X-Participant-Token when available', async () => {
      persistence.saveParticipationData('018e3a2b-8888-7def-8123-000000000001', 'ptk_123', 'Léa')
      const spy = jest.spyOn(sdk, 'getScores').mockResolvedValue({
        data: {
          sessionId: '018e3a2b-8888-7def-8123-000000000001',
          slideIndex: 3,
          leaderboard: [
            { rank: 1, participantName: 'Léa', score: 2500 },
            { rank: 2, participantName: 'Zoé', score: 1800 },
          ],
        },
        error: undefined,
        request: {} as never,
        response: { status: 200 } as never,
      })

      const result = await firstValueFrom(service.getSessionScores('018e3a2b-8888-7def-8123-000000000001'))
      expect(spy).toHaveBeenCalledWith(expect.objectContaining({
        path: { id: '018e3a2b-8888-7def-8123-000000000001' },
        headers: { 'X-Participant-Token': 'ptk_123' },
      }))
      expect(isSuccess(result)).toBe(true)
      if (isSuccess(result)) {
        expect(result.result.leaderboard).toHaveLength(2)
        expect(result.result.leaderboard?.[0].participantName).toBe('Léa')
      }
    })

    it('should return NotFoundError on 404', async () => {
      jest.spyOn(sdk, 'getScores').mockResolvedValue({
        data: undefined,
        error: { message: 'Not found' },
        request: {} as never,
        response: { status: 404 } as never,
      })

      const result = await firstValueFrom(service.getSessionScores('018e3a2b-8888-7def-8123-000000000001'))
      expect(isFailure(result)).toBe(true)
      if (isFailure(result)) {
        expect(result.error).toBeInstanceOf(NotFoundError)
      }
    })
  })
})

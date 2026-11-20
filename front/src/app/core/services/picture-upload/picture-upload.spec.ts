import { TestBed } from '@angular/core/testing'

import { of } from 'rxjs'

import * as sdk from '@quezap/core/api-client/sdk.gen'
import { isFailure, isSuccess } from '@quezap/core/types'
import { PictureUrl } from '@quezap/domain/models'

import { PictureUploadService } from './picture-upload'

describe('PictureUploadService', () => {
  let service: PictureUploadService

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [PictureUploadService] })
    service = TestBed.inject(PictureUploadService)
  })

  afterEach(() => {
    jest.restoreAllMocks()
  })

  it('should upload picture successfully and return public URL', (done) => {
    const file = new File(['fake-image'], 'photo.png', { type: 'image/png' })
    const mockResponse = {
      data: {
        pictureUrl: '/quezap-public-images/pictures/questions/uuid.png',
        objectKey: 'pictures/questions/uuid.png',
        expiresAt: '2026-11-02T12:00:00Z',
      },
      error: undefined,
    }

    jest.spyOn(sdk, 'upload').mockReturnValue(of(mockResponse) as never)

    service.upload(file).subscribe((output) => {
      expect(isSuccess(output)).toBe(true)
      if (isSuccess(output)) {
        expect(output.result.pictureUrl).toBe(
          '/quezap-public-images/pictures/questions/uuid.png' as PictureUrl,
        )
        expect(output.result.objectKey).toBe('pictures/questions/uuid.png')
      }
      done()
    })
  })

  it('should return failure when sdk returns error with status 413', (done) => {
    const file = new File(['fake-image'], 'photo.png', { type: 'image/png' })
    jest.spyOn(sdk, 'upload').mockReturnValue(
      of({ data: undefined, error: {}, response: { status: 413 } }) as never,
    )

    service.upload(file).subscribe((output) => {
      expect(isFailure(output)).toBe(true)
      if (isFailure(output)) {
        expect(output.error.message).toContain('5 Mo')
      }
      done()
    })
  })

  it('should reject unsupported MIME type before calling sdk', (done) => {
    const spy = jest.spyOn(sdk, 'upload')
    const file = new File(['gif-bytes'], 'anim.gif', { type: 'image/gif' })

    service.upload(file).subscribe((output) => {
      expect(isFailure(output)).toBe(true)
      expect(spy).not.toHaveBeenCalled()
      done()
    })
  })

  it('should reject file larger than 5 Mo before calling sdk', (done) => {
    const spy = jest.spyOn(sdk, 'upload')
    const oversized = new File(['x'], 'big.png', { type: 'image/png' })
    Object.defineProperty(oversized, 'size', { value: 5 * 1024 * 1024 + 1 })

    service.upload(oversized).subscribe((output) => {
      expect(isFailure(output)).toBe(true)
      expect(spy).not.toHaveBeenCalled()
      done()
    })
  })

  it('should map 401 and 415 HTTP errors to explicit messages', (done) => {
    const file = new File(['fake-image'], 'photo.webp', { type: 'image/webp' })
    jest.spyOn(sdk, 'upload').mockReturnValue(
      of({ data: undefined, error: {}, response: { status: 401 } }) as never,
    )

    service.upload(file).subscribe((output) => {
      expect(isFailure(output)).toBe(true)
      if (isFailure(output)) {
        expect(output.error.message).toContain('Session expirée')
      }
      done()
    })
  })
})

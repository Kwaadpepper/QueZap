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

  it('should return failure when sdk returns error', (done) => {
    const file = new File(['fake-image'], 'photo.png', { type: 'image/png' })
    jest.spyOn(sdk, 'upload').mockReturnValue(
      of({ data: undefined, error: { message: 'Upload failed' } }) as never,
    )

    service.upload(file).subscribe((output) => {
      expect(isFailure(output)).toBe(true)
      done()
    })
  })
})

import { Injectable } from '@angular/core'

import { catchError, from, map, of } from 'rxjs'

import { PictureUploadResponseDto } from '@quezap/core/api-client'
import { upload } from '@quezap/core/api-client/sdk.gen'
import { ValidationError } from '@quezap/core/errors'
import { ServiceOutput } from '@quezap/core/types'
import { PictureUrl } from '@quezap/domain/models'

export interface UploadedPicture {
  pictureUrl: PictureUrl
  objectKey: string
  expiresAt: string
}

@Injectable({ providedIn: 'root' })
export class PictureUploadService {
  upload(file: File): ServiceOutput<UploadedPicture, ValidationError> {
    return from(
      upload({ body: { file } }),
    ).pipe(
      map(({ data, error }) => {
        if (error || !data) {
          return {
            kind: 'failure' as const,
            error: new ValidationError({}, 'Échec du téléversement de l’image'),
          }
        }

        const dto = data as PictureUploadResponseDto
        if (!dto.pictureUrl) {
          return {
            kind: 'failure' as const,
            error: new ValidationError({}, 'Réponse d’upload invalide'),
          }
        }

        return {
          kind: 'success' as const,
          result: {
            pictureUrl: dto.pictureUrl as PictureUrl,
            objectKey: dto.objectKey ?? '',
            expiresAt: dto.expiresAt ?? '',
          },
        }
      }),
      catchError((err: unknown) =>
        of({
          kind: 'failure' as const,
          error: new ValidationError(
            {},
            err instanceof Error ? err.message : 'Erreur réseau lors du téléversement',
          ),
        }),
      ),
    )
  }
}

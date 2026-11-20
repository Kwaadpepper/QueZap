import { Injectable } from '@angular/core'

import { catchError, from, map, of } from 'rxjs'

import { upload } from '@quezap/core/api-client/sdk.gen'
import { zPictureUploadResponseDto } from '@quezap/core/api-client/zod.gen'
import { ValidationError } from '@quezap/core/errors'
import { ServiceOutput } from '@quezap/core/types'
import { PictureUrl } from '@quezap/domain/models'

const ALLOWED_MIME_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])
const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024

export interface UploadedPicture {
  pictureUrl: PictureUrl
  objectKey: string
  expiresAt: string
}

function extractApiErrorMessage(error: unknown): string | undefined {
  if (typeof error === 'string' && error.trim().length > 0) {
    return error
  }
  if (error && typeof error === 'object') {
    const record = error as Record<string, unknown>
    if (typeof record['message'] === 'string' && record['message'].trim().length > 0) {
      return record['message']
    }
    if (typeof record['detail'] === 'string' && record['detail'].trim().length > 0) {
      return record['detail']
    }
  }
  return undefined
}

function mapUploadHttpError(status: number | undefined, error: unknown): string {
  const apiMessage = extractApiErrorMessage(error)
  switch (status) {
    case 400:
      return apiMessage ?? 'Requête invalide lors du téléversement de l’image.'
    case 401:
      return 'Session expirée. Veuillez vous reconnecter.'
    case 413:
      return 'L’image dépasse la taille maximale autorisée (5 Mo).'
    case 415:
      return 'Format d’image non supporté (JPEG, PNG ou WEBP requis).'
    default:
      if (status !== undefined && status >= 500) {
        return 'Erreur serveur lors du téléversement de l’image.'
      }
      if (status === 0) {
        return 'Erreur réseau lors du téléversement de l’image.'
      }
      return apiMessage ?? 'Échec du téléversement de l’image.'
  }
}

@Injectable({ providedIn: 'root' })
export class PictureUploadService {
  upload(file: File): ServiceOutput<UploadedPicture, ValidationError> {
    if (!ALLOWED_MIME_TYPES.has(file.type)) {
      return of({
        kind: 'failure' as const,
        error: new ValidationError({}, 'Format non supporté. Formats acceptés : JPEG, PNG, WEBP.'),
      })
    }

    if (file.size > MAX_FILE_SIZE_BYTES) {
      return of({
        kind: 'failure' as const,
        error: new ValidationError({}, 'L’image dépasse la taille maximale autorisée (5 Mo).'),
      })
    }

    return from(
      upload({ body: { file } }),
    ).pipe(
      map((result) => {
        const { data, error } = result
        const status = result.response?.status
        if (error || !data) {
          const message = mapUploadHttpError(status, error)
          return {
            kind: 'failure' as const,
            error: new ValidationError({}, message),
          }
        }

        const parsed = zPictureUploadResponseDto.safeParse(data)
        if (!parsed.success || !parsed.data.pictureUrl) {
          return {
            kind: 'failure' as const,
            error: new ValidationError({}, 'Réponse d’upload invalide'),
          }
        }

        return {
          kind: 'success' as const,
          result: {
            pictureUrl: parsed.data.pictureUrl as PictureUrl,
            objectKey: parsed.data.objectKey ?? '',
            expiresAt: parsed.data.expiresAt ?? '',
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

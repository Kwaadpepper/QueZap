import { inject } from '@angular/core'
import { RedirectCommand, ResolveFn, Router } from '@angular/router'

import { catchError, map, of } from 'rxjs'

import { NotFoundError } from '@quezap/core/errors'
import { isFailure } from '@quezap/core/types'
import { QuezapId, QuezapWithQuestionsAndAnswers } from '@quezap/domain/models'

import { QUEZAP_SERVICE } from '../../services'

const notFoundUrl = 'not-found'
const redirectUrl = '/admin/quezaps'

export const quezapResolver: ResolveFn<QuezapWithQuestionsAndAnswers | RedirectCommand> = (route) => {
  const quezapService = inject(QUEZAP_SERVICE)
  const router = inject(Router)
  const redirectionOnNotFound = new RedirectCommand(router.parseUrl(notFoundUrl))
  const redirectionOnError = new RedirectCommand(router.parseUrl(redirectUrl))

  const quezapId = route.paramMap.get('quezap')!

  return quezapService.find(quezapId as QuezapId).pipe(
    map((output) => {
      if (isFailure(output)) {
        const err = output.error
        if (err instanceof NotFoundError) {
          return redirectionOnNotFound
        }

        throw err
      }

      return output.result
    }),
    catchError(() => of(redirectionOnError)),
  )
}

import { HttpInterceptorFn } from '@angular/common/http'
import { inject } from '@angular/core'

import { finalize } from 'rxjs'

import { LoadingStatus } from '@quezap/core/services/loading/loading-status'

export const loadingProgressionInterceptor: HttpInterceptorFn = (req, next) => {
  const progression = inject(LoadingStatus)

  progression.start()

  const response$ = next(req)

  return response$.pipe(
    finalize(() => {
      progression.stop()
    }),
  )
}

import { HttpErrorResponse } from '@angular/common/http'
import { ErrorHandler, inject, Injectable } from '@angular/core'

import { Config } from '../services'

import { ERROR_NOTIFIER } from './error-notifier'

@Injectable({
  providedIn: 'root',
})
export class GlobalErrorHandler implements ErrorHandler {
  private readonly config = inject(Config)
  private readonly notifier = inject(ERROR_NOTIFIER)

  handleError(error: unknown): void {
    console.warn('--- Unhandled error detected ---')

    let summary = 'Une erreur est survenue'
    let detail = 'Veuillez réessayer plus tard.'

    if (this.config.debug()) {
      switch (true) {
        case error instanceof HttpErrorResponse:
          summary = `HTTP Error ${error.status}`
          detail = error.message
          break
        case error instanceof Error:
          summary = 'Application Error'
          detail = error.message
          break
      }
    }

    this.notifier.notify(summary, detail)

    // TODO: add an external loggin service here
  }
}

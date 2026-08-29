import { HttpErrorResponse } from '@angular/common/http'
import { ErrorHandler, inject, Injectable } from '@angular/core'

import { Config } from '../services/config/config'
import { ExternalLoggerService } from '../services/logger/external-logger'

import { ERROR_NOTIFIER } from './error-notifier'
import { HandledFrontError } from './handled-front-error'

@Injectable({ providedIn: 'root' })
export class GlobalErrorHandler implements ErrorHandler {
  private readonly config = inject(Config)
  private readonly notifier = inject(ERROR_NOTIFIER)
  private readonly externalLogger = inject(ExternalLoggerService)

  // * Arrow function to preserve "this" context when passed as callback
  handleError = (error: unknown): void => {
    let summary = 'Une erreur est survenue'
    let detail = 'Veuillez réessayer plus tard.'

    if (this.config.debug()) {
      switch (true) {
        case error instanceof HandledFrontError:
          summary = 'Handled Error'
          detail = error.message
          break
        case error instanceof HttpErrorResponse:
          summary = `HTTP Error ${error.status}`
          detail = error.message
          break
        case error instanceof Error:
          summary = 'Application Error'
          detail = error.message
          break
      }

      if (error instanceof HandledFrontError) {
        console.debug('--- Handled error detected --- \n'
          + `Summary: ${summary}\n`
          + `Detail: ${detail}\n`,
        )
      }
      else {
        console.warn('--- Unhandled error detected ---')
        this.notifier.notify(summary, detail)
        console.error(error)
      }
    }
    else {
      console.error('GlobalErrorHandler caught an error:', error)
    }

    this.externalLogger.logError(error, summary, detail)
  }
}

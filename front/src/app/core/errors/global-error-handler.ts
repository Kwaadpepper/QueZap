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

    if (error instanceof HttpErrorResponse) {
      summary = `Erreur réseau (${error.status})`
      detail = error.error?.message || error.statusText || 'Impossible de contacter le serveur.'
    }
    else if (error instanceof HandledFrontError) {
      summary = 'Information'
      detail = error.message
    }
    else if (error instanceof Error) {
      summary = this.config.debug() ? 'Erreur applicative' : 'Une erreur est survenue'
      detail = this.config.debug() ? error.message : 'Une action inattendue s\'est produite.'
    }

    if (this.config.debug()) {
      if (error instanceof HandledFrontError) {
        console.debug('--- Handled error detected --- \n'
          + `Summary: ${summary}\n`
          + `Detail: ${detail}\n`,
        )
      }
      else {
        console.warn('--- Unhandled error detected ---', error)
        this.notifier.notify(summary, detail)
      }
    }
    else {
      if (!(error instanceof HandledFrontError)) {
        this.notifier.notify(summary, detail)
      }
      console.error('GlobalErrorHandler caught an error:', error)
    }

    this.externalLogger.logError(error, summary, detail)
  }
}

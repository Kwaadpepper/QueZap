import { InjectionToken } from '@angular/core'

import { MessageService } from 'primeng/api'

export interface ErrorNotifier {
  notify(summary: string, detail: string): void
}

export const ERROR_NOTIFIER = new InjectionToken<ErrorNotifier>('ErrorNotifierToken')

export function createErrorNotifier(messageService: MessageService): ErrorNotifier {
  return {
    notify: (summary: string, detail: string) => {
      console.error('Notifying error:', summary, detail)
      messageService.add({
        severity: 'error',
        summary,
        detail,
        life: 8000,
      })
    },
  }
}

import { Injectable } from '@angular/core'

@Injectable({ providedIn: 'root' })
export class ExternalLoggerService {
  logError(error: unknown, summary: string, detail: string): void {
    // Simulation d'un appel vers un service externe (ex: Sentry, Datadog)
    console.debug('[ExternalLogger] Envoi erreur', { summary, detail, error })
  }
}

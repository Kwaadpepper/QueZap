import { DOCUMENT, inject } from '@angular/core'

import { catchError, firstValueFrom, forkJoin, tap, throwError } from 'rxjs'

import { Config } from './core/services'
import { AuthenticatedUserStore } from './shared/stores'

export async function AppInitializer() {
  const config = inject(Config)
  const authStore = inject(AuthenticatedUserStore)
  const document = inject(DOCUMENT)

  // Init tasks
  const initializationTasks = [
    authStore.loadInitialState(),
  ]

  const initialization$ = forkJoin(initializationTasks).pipe(
    catchError((err) => {
      if (config.debug()) {
        console.warn('AppInitializer: Failed to load initial state (one or more tasks failed).', err)
      }

      if (confirm('Une erreur est survenue lors du démarrage de l\'application. Veuillez essayer de rafraîchir la page.')) {
        globalThis.location.reload()
      }

      return throwError(() => err)
    }),
    tap(() => HideSplashScreen(document)),
  )

  return await firstValueFrom(initialization$)
}

function HideSplashScreen(document: Document) {
  const splashScreen = document.getElementById('app-loading-screen')
  if (splashScreen) {
    splashScreen.style.opacity = '0'

    setTimeout(() => {
      splashScreen.remove()
    }, 300)
  }
  return Promise.resolve()
}

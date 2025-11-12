import { DOCUMENT, inject } from '@angular/core'

import { catchError, firstValueFrom, Subject, tap } from 'rxjs'

import { Config } from './core/services'
import { AuthenticatedUserStore } from './shared/stores'

export function AppInitializer() {
  const config = inject(Config)
  const authStore = inject(AuthenticatedUserStore)
  const bootLoader = new Subject<void>()

  const document = inject(DOCUMENT)

  setTimeout(() => {
    bootLoader.next()
  }, 0)

  return firstValueFrom(bootLoader.pipe(
    tap(authStore.loadInitialState()),
    catchError(() => {
      if (config.debug()) {
        console.warn('AppInitializer: Failed to load initial authenticated user state.')
      }
      return []
    }),
    tap(() => HideSplashScreen(document)),
  ))
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

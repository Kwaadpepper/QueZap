import { DOCUMENT, inject } from '@angular/core'

import {
  catchError, firstValueFrom, forkJoin,
  of, tap,
} from 'rxjs'

import { client } from './core/api-client/client.gen'
import { Config } from './core/services/config/config'
import { AuthenticatedUserStore } from './shared/stores/authenticated-user'
import { initZod } from './zod.initializer'

export async function AppInitializer() {
  const config = inject(Config)
  const authStore = inject(AuthenticatedUserStore)
  const document = inject(DOCUMENT)

  // Configure Hey API client
  const apiUrl = config.appConfig.value().apiUrl
  const baseHost = apiUrl.replace(/\/apiv1\/?$/, '')
  client.setConfig({
    baseUrl: typeof window !== 'undefined' && !baseHost.startsWith('http')
      ? `${window.location.origin}${baseHost}`
      : baseHost,
  })

  // Init tasks
  const initializationTasks = [
    initZod(),
    authStore.loadInitialState().pipe(
      catchError((err) => {
        if (config.debug()) {
          console.warn('AppInitializer: Auth initialization failed, continuing as guest.', err)
        }
        return of(void 0)
      }),
    ),
  ]

  const initialization$ = forkJoin(initializationTasks).pipe(
    catchError((err) => {
      if (config.debug()) {
        console.warn('AppInitializer: Failed to load initial state (one or more tasks failed).', err)
      }
      return of(void 0)
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

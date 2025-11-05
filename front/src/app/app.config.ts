import { ApplicationConfig, enableProdMode, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core'
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async'
import { provideRouter } from '@angular/router'

import { providePrimeNG } from 'primeng/config'

import { environment } from '@quezap/env/environment'
import Quezap from '@quezap/themes/Quezap'

import { routes } from './app.routes'
import { ConfigService } from './core/services/config'

if (environment.env === 'prod') {
  enableProdMode()
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: Quezap,
        options: {
          cssLayer: {
            name: 'primeng',
            order: 'theme, base, primeng',
          },
        },
      },
    }),
    ConfigService,
  ],
}

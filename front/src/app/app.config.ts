import { provideHttpClient } from '@angular/common/http'
import { ApplicationConfig, enableProdMode, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core'
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async'
import { provideRouter, TitleStrategy } from '@angular/router'

import { providePrimeNG } from 'primeng/config'

import { Config } from '@quezap/core/services'
import { environment } from '@quezap/env/environment'
import Quezap from '@quezap/themes/Quezap'

import { routes } from './app.routes'
import { DynamicTitleStrategy } from './core/strategies'
import { ThemeMockService } from './features/admin/themes/services'
import { THEME_SERVICE } from './features/admin/themes/services/theme'

if (environment.env === 'prod') {
  enableProdMode()
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideAnimationsAsync(),
    provideHttpClient(),
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
    {
      provide: Config,
      useClass: Config,
    },
    {
      provide: TitleStrategy,
      useClass: DynamicTitleStrategy,
    },
    {
      provide: THEME_SERVICE,
      useClass: ThemeMockService,
    },
  ],
}

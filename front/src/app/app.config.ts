import { provideHttpClient } from '@angular/common/http'
import {
  ApplicationConfig, enableProdMode, ErrorHandler, provideAppInitializer,
  provideBrowserGlobalErrorListeners, provideZonelessChangeDetection,
} from '@angular/core'
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async'
import { provideRouter, TitleStrategy } from '@angular/router'

import { MessageService } from 'primeng/api'
import { providePrimeNG } from 'primeng/config'

import { environment } from '@quezap/env/environment'
import Quezap from '@quezap/themes/Quezap'

import { AppInitializer } from './app.initializer'
import { routes } from './app.routes'
import { createErrorNotifier, ERROR_NOTIFIER, GlobalErrorHandler } from './core/errors'
import { Config } from './core/services/config/config'
import { DynamicTitleStrategy } from './core/strategies/dynamic-title'
import { AUTHENTICATION_SERVICE, AuthenticationMockService } from './features/auth/services'

if (environment.env === 'prod') {
  enableProdMode()
}

const appProviders: ApplicationConfig['providers'] = [
  MessageService,
  { provide: Config, useClass: Config },
  { provide: ERROR_NOTIFIER, useFactory: createErrorNotifier, deps: [MessageService] },
  { provide: ErrorHandler, useClass: GlobalErrorHandler },
  { provide: AUTHENTICATION_SERVICE, useClass: AuthenticationMockService },
  { provide: TitleStrategy, useClass: DynamicTitleStrategy },
]

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(),
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
    provideAppInitializer(AppInitializer),
    ...appProviders,
  ],
}

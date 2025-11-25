import { computed, Injectable, resource, ResourceRef } from '@angular/core'

import * as zod from 'zod/v4'

import { environment } from '@quezap/env/environment'

export enum Environment {
  DEV = 'dev',
  PROD = 'prod',
}

const configSchema = zod.object({
  env: zod.enum(Environment),
  useMockData: zod.boolean(),
  appName: zod.string(),
  authorName: zod.string(),
  authorEmail: zod.email(),
  apiUrl: zod.url(),
})

type AppConfig = zod.infer<typeof configSchema>

@Injectable({ providedIn: 'root' })
export class Config {
  public readonly appConfig: ResourceRef<AppConfig> = resource({
    defaultValue: configSchema.parse({
      env: Environment.PROD,
      useMockData: false,
      appName: 'Quizz',
      authorName: 'Example Author',
      authorEmail: 'example@example.net',
      apiUrl: 'https://example.net',
    }),
    loader: () => Promise.resolve(configSchema.parse(environment)),
  })

  public readonly debug = computed(() => {
    return this.appConfig.value().env === Environment.DEV
  })
}

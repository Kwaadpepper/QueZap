import { computed, Injectable, resource, ResourceRef } from '@angular/core'

import z from 'zod'

import { environment } from '@quezap/env/environment'

export enum Environment {
  DEV = 'dev',
  PROD = 'prod',
}

const configSchema = z.object({
  env: z.enum(Environment),
  useMockData: z.boolean(),
  appName: z.string(),
  authorName: z.string(),
  authorEmail: z.email(),
  apiUrl: z.url(),
})

type AppConfig = z.infer<typeof configSchema>

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

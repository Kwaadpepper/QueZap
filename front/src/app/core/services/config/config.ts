import { computed, Injectable, Signal, signal } from '@angular/core'

import * as zod from 'zod/v4'

import { environment } from '@quezap/env/environment'

export enum Environment {
  DEV = 'dev',
  PROD = 'prod',
}

const appNameSchema = zod.string().trim().min(1)
const authorNameSchema = zod.string().trim().min(1)
const authorEmailSchema = zod.email()
const apiUrlSchema = zod.union([zod.url(), zod.string().regex(/^\/.*$/)])
const turnstileSiteKeySchema = zod.string().trim().min(1)

const configSchema = zod.object({
  env: zod.enum(Environment),
  useMockData: zod.boolean(),
  appName: appNameSchema,
  authorName: authorNameSchema,
  authorEmail: authorEmailSchema,
  apiUrl: apiUrlSchema,
  turnstileSiteKey: turnstileSiteKeySchema.optional(),
})

export type AppConfig = zod.infer<typeof configSchema>

const RUNTIME_CONFIG_URL = '/config.json'

@Injectable({ providedIn: 'root' })
export class Config {
  private readonly configSignal = signal<AppConfig>(configSchema.parse(environment))

  public readonly appConfig: { readonly value: Signal<AppConfig> } = { value: this.configSignal.asReadonly() }

  public readonly debug = computed(() => {
    return this.appConfig.value().env === Environment.DEV
  })

  public async load(): Promise<AppConfig> {
    if (typeof globalThis.fetch !== 'function') {
      return this.configSignal()
    }

    try {
      const response = await globalThis.fetch(RUNTIME_CONFIG_URL, { cache: 'no-store' })
      if (!response.ok) {
        return this.configSignal()
      }

      const contentType = response.headers.get('content-type') ?? ''
      if (!contentType.includes('application/json')) {
        return this.configSignal()
      }

      const raw: unknown = await response.json()
      if (typeof raw !== 'object' || raw === null || Array.isArray(raw)) {
        return this.configSignal()
      }

      const record = raw as Record<string, unknown>
      const current = this.configSignal()
      const overrides: Partial<AppConfig> = {}

      if (record['appName'] !== undefined && record['appName'] !== '') {
        const parsed = appNameSchema.safeParse(record['appName'])
        if (parsed.success) {
          overrides.appName = parsed.data
        }
        else {
          console.warn('Config: Ignoring invalid runtime appName override.')
        }
      }

      if (record['authorName'] !== undefined && record['authorName'] !== '') {
        const parsed = authorNameSchema.safeParse(record['authorName'])
        if (parsed.success) {
          overrides.authorName = parsed.data
        }
        else {
          console.warn('Config: Ignoring invalid runtime authorName override.')
        }
      }

      if (record['authorEmail'] !== undefined && record['authorEmail'] !== '') {
        const parsed = authorEmailSchema.safeParse(record['authorEmail'])
        if (parsed.success) {
          overrides.authorEmail = parsed.data
        }
        else {
          console.warn('Config: Ignoring invalid runtime authorEmail override.')
        }
      }

      if (record['apiUrl'] !== undefined && record['apiUrl'] !== '') {
        const parsed = apiUrlSchema.safeParse(record['apiUrl'])
        if (parsed.success) {
          overrides.apiUrl = parsed.data
        }
        else {
          console.warn('Config: Ignoring invalid runtime apiUrl override.')
        }
      }

      if (record['turnstileSiteKey'] !== undefined && record['turnstileSiteKey'] !== '') {
        const parsed = turnstileSiteKeySchema.safeParse(record['turnstileSiteKey'])
        if (parsed.success) {
          overrides.turnstileSiteKey = parsed.data
        }
        else {
          console.warn('Config: Ignoring invalid runtime turnstileSiteKey override.')
        }
      }

      const merged = configSchema.parse({
        ...current,
        ...overrides,
      })
      this.configSignal.set(merged)
      return merged
    }
    catch {
      return this.configSignal()
    }
  }
}

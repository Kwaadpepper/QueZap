import fs from 'node:fs'
import path from 'node:path'

import { Browser, BrowserContext, expect, Page } from '@playwright/test'

export const AUDIT_SCREENSHOT_DIR = '/tmp/quezap-e2e-audit'

export interface ErrorTracker {
  pageErrors: string[]
  consoleErrors: string[]
  assertNoErrors: (allowedConsoleSubstrings?: string[]) => void
}

export function attachErrorTracker(page: Page, label: string): ErrorTracker {
  const pageErrors: string[] = []
  const consoleErrors: string[] = []

  page.on('pageerror', (err) => {
    pageErrors.push(`[${label}] pageerror: ${err.message}`)
  })

  page.on('console', (msg) => {
    if (msg.type() === 'error') {
      consoleErrors.push(`[${label}] console.error: ${msg.text()}`)
    }
  })

  return {
    pageErrors,
    consoleErrors,
    assertNoErrors: (allowedConsoleSubstrings: string[] = []) => {
      expect(pageErrors, `Uncaught page errors on ${label}`).toEqual([])
      const unexpectedConsole = consoleErrors.filter(
        err => !allowedConsoleSubstrings.some(sub => err.includes(sub)),
      )
      expect(unexpectedConsole, `Unexpected console errors on ${label}`).toEqual([])
    },
  }
}

export async function saveAuditScreenshot(page: Page, filename: string): Promise<void> {
  fs.mkdirSync(AUDIT_SCREENSHOT_DIR, { recursive: true })
  await page.screenshot({ path: path.join(AUDIT_SCREENSHOT_DIR, filename) })
}

export async function apiCall(
  base: string,
  endpoint: string,
  method = 'GET',
  body?: unknown,
  token?: string,
  headers: Record<string, string> = {},
): Promise<{ status: number; json: Record<string, unknown> | null; text: string }> {
  const res = await fetch(`${base}/apiv1${endpoint}`, {
    method,
    headers: {
      ...(body !== undefined ? { 'content-type': 'application/json' } : {}),
      ...(token ? { authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  const text = await res.text()
  let json: Record<string, unknown> | null = null
  try {
    json = JSON.parse(text) as Record<string, unknown>
  }
  catch {
    json = null
  }
  return { status: res.status, json, text }
}

export async function loginAsAdminApi(base: string): Promise<string> {
  const res = await apiCall(base, '/auth/login', 'POST', {
    identifier: 'admin@quezap.com',
    password: 'Password123!',
  })
  expect(res.status, `Admin login failed: ${res.text}`).toBe(200)
  const token = String(res.json?.['accessToken'] || '')
  expect(token).toBeTruthy()
  return token
}

export async function loginAsAdminUi(page: Page, base: string): Promise<void> {
  await page.goto(`${base}/auth/login`)
  await page.locator('input[type="password"]').first().fill('Password123!')
  await page.locator('input').first().fill('admin@quezap.com')
  await page.keyboard.press('Enter')
  await page.waitForURL(/\/admin/, { timeout: 15000 })
}

export async function createMobileStudent(
  browser: Browser,
  base: string,
  sessionCode: string,
  nickname: string,
): Promise<{ context: BrowserContext; page: Page; tracker: ErrorTracker }> {
  const context = await browser.newContext({
    viewport: { width: 390, height: 844 },
    isMobile: true,
    hasTouch: true,
  })
  const page = await context.newPage()
  const tracker = attachErrorTracker(page, `student:${nickname}`)
  await page.goto(`${base}/quizz/join/${sessionCode}`)
  await page.locator('#nickname-value').fill(nickname)
  await page.locator('button[type="submit"]').click()
  await page.waitForSelector('quizz-participant-list', { timeout: 15000 })
  return { context, page, tracker }
}

/**
 * Generates a valid 64x64 solid-color PNG buffer (with valid PNG signature, IHDR, IDAT, IEND).
 */
export function createValidPngBuffer(): Buffer {
  // Minimal valid 1x1 RGBA PNG
  return Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
    'base64',
  )
}

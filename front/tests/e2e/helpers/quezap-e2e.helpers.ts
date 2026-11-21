import fs from 'node:fs'
import path from 'node:path'
import { deflateSync } from 'node:zlib'

import { Browser, BrowserContext, expect, Locator, Page } from '@playwright/test'

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
export function createValidPngBuffer(width = 200, height = 120): Buffer {
  // Real, clearly visible RGB PNG (horizontal colour bands) generated without any dependency
  const crcTable = Array.from({ length: 256 }, (_, n) => {
    let c = n
    for (let k = 0; k < 8; k++) {
      c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1
    }
    return c >>> 0
  })
  const crc32 = (buf: Buffer): number => {
    let c = 0xffffffff
    for (const byte of buf) {
      c = crcTable[(c ^ byte) & 0xff] ^ (c >>> 8)
    }
    return (c ^ 0xffffffff) >>> 0
  }
  const chunk = (type: string, data: Buffer): Buffer => {
    const length = Buffer.alloc(4)
    length.writeUInt32BE(data.length)
    const body = Buffer.concat([Buffer.from(type, 'ascii'), data])
    const crc = Buffer.alloc(4)
    crc.writeUInt32BE(crc32(body))
    return Buffer.concat([length, body, crc])
  }
  const ihdr = Buffer.alloc(13)
  ihdr.writeUInt32BE(width, 0)
  ihdr.writeUInt32BE(height, 4)
  ihdr[8] = 8 // bit depth
  ihdr[9] = 2 // RGB
  const bands = [[239, 68, 68], [250, 204, 21], [34, 197, 94], [59, 130, 246]]
  const rows: Buffer[] = []
  for (let y = 0; y < height; y++) {
    const [r, g, b] = bands[Math.floor((y * bands.length) / height)]
    const row = Buffer.alloc(1 + width * 3)
    for (let x = 0; x < width; x++) {
      row[1 + x * 3] = r
      row[2 + x * 3] = g
      row[3 + x * 3] = b
    }
    rows.push(row)
  }
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', deflateSync(Buffer.concat(rows))),
    chunk('IEND', Buffer.alloc(0)),
  ])
}

export async function expectImageRendered(locator: Locator, minWidth = 80, minHeight = 60): Promise<void> {
  await expect(locator).toBeVisible({ timeout: 15000 })
  await expect.poll(
    () => locator.evaluate((img: HTMLImageElement) => img.complete && img.naturalWidth > 0),
    { timeout: 15000 },
  ).toBe(true)
  const box = await locator.boundingBox()
  expect(box).not.toBeNull()
  expect(box!.width).toBeGreaterThanOrEqual(minWidth)
  expect(box!.height).toBeGreaterThanOrEqual(minHeight)
}

/** Waits for podium CSS animations / transitions to end, then asserts nothing overlaps or is clipped. */
export async function expectPodiumLayoutClean(page: Page): Promise<void> {
  await page.evaluate(async () => {
    const finite = () => document.getAnimations().filter(a => a.effect?.getTiming().iterations !== Infinity)
    for (let i = 0; i < 5 && finite().length > 0; i++) {
      await Promise.allSettled(finite().map(a => a.finished))
    }
  })
  const rects = await page.evaluate(() => {
    const toRect = (el: Element) => {
      const r = el.getBoundingClientRect()
      return { x: r.x, y: r.y, w: r.width, h: r.height, right: r.right, bottom: r.bottom }
    }
    const blocks = [...document.querySelectorAll('main.relative > *')]
      .filter(el => el.querySelector('.podium-step') || el.matches('.podium-box') || (el.textContent ?? '').trim().length > 0)
      .filter(el => !(el instanceof HTMLCanvasElement) && !el.classList.contains('confetti-piece'))
      .map(el => ({ name: el.className.toString().slice(0, 40), ...toRect(el) }))
    const steps = [...document.querySelectorAll('.podium-step')].map(el => {
      const r = toRect(el)
      const clipped = [...el.querySelectorAll('*')].some((c) => {
        const cr = c.getBoundingClientRect()
        return cr.width > 0 && (cr.bottom > r.bottom + 1 || cr.right > r.right + 1 || cr.top < r.y - 1 || cr.x < r.x - 1)
      })
      return { ...r, clipped }
    })
    return { blocks, steps, vh: window.innerHeight }
  })
  for (const step of rects.steps) {
    expect(step.clipped, 'podium step content must stay inside its step').toBe(false)
  }
  const sorted = [...rects.blocks].sort((a, b) => a.y - b.y)
  for (let i = 1; i < sorted.length; i++) {
    expect(
      sorted[i].y,
      `block "${sorted[i].name}" overlaps "${sorted[i - 1].name}"`,
    ).toBeGreaterThanOrEqual(sorted[i - 1].bottom - 1)
  }
  for (const block of sorted) {
    expect(block.bottom, `block "${block.name}" is cut off by the viewport`).toBeLessThanOrEqual(rects.vh + 1)
  }
}

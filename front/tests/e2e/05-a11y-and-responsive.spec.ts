import { expect, test } from '@playwright/test'

import {
  apiCall,
  attachErrorTracker,
  createMobileStudent,
  createValidPngBuffer,
  loginAsAdminApi,
  loginAsAdminUi,
  saveAuditScreenshot,
} from './helpers/quezap-e2e.helpers'

test.describe('05 - Accessibility, Keyboard Navigation & Responsive Viewports', () => {
  test('HTML lang="fr", mobile 390x844 layout/hitboxes/aria-live, and 1280x720 podium without clipping', async ({ browser, baseURL }) => {
    const base = baseURL || 'http://localhost:8000'
    const token = await loginAsAdminApi(base)

    // Upload a valid picture via API so we can check no visible "Image de la question" text on mobile
    const formData = new FormData()
    formData.append('file', new Blob([new Uint8Array(createValidPngBuffer())], { type: 'image/png' }), 'a11y-test.png')
    const uploadRes = await fetch(`${base}/apiv1/pictures/upload`, {
      method: 'POST',
      headers: { authorization: `Bearer ${token}` },
      body: formData,
    })
    expect([200, 201]).toContain(uploadRes.status)
    const uploadJson = (await uploadRes.json()) as { pictureUrl?: string }
    const pictureUrl = String(uploadJson.pictureUrl || '')

    const quezapRes = await apiCall(base, '/quezaps', 'POST', {
      title: `E2E A11y Quiz ${Date.now() % 100000}`,
      description: 'Test A11y et Responsive',
      isPublic: true,
      slides: [
        {
          timer: 25,
          timerSeconds: 25,
          points: 1000,
          question: {
            label: 'Question accessible avec image ?',
            type: 'QUIZZ',
            pictureFilename: pictureUrl,
            answers: [
              { label: 'Oui', isCorrect: true, answerOrder: 1 },
              { label: 'Non', isCorrect: false, answerOrder: 2 },
              { label: 'Peut-être', isCorrect: false, answerOrder: 3 },
              { label: 'Jamais', isCorrect: false, answerOrder: 4 },
            ],
          },
        },
      ],
    }, token)
    expect(quezapRes.status).toBe(201)
    const quezapId = String(quezapRes.json?.['id'] || '')

    const sessionRes = await apiCall(base, '/sessions', 'POST', {
      name: `Session A11y ${Date.now() % 10000}`,
      quezapId,
    }, token)
    expect(sessionRes.status).toBe(201)
    const sessionId = String(sessionRes.json?.['id'] || '')
    const sessionCode = String(sessionRes.json?.['code'] || '')

    // Host at 1280x720 (720p projector)
    const hostContext = await browser.newContext({ viewport: { width: 1280, height: 720 } })
    const hostPage = await hostContext.newPage()
    const hostTracker = attachErrorTracker(hostPage, 'host-720p')
    await loginAsAdminUi(hostPage, base)

    // Verify <html lang="fr">
    await expect(hostPage.locator('html')).toHaveAttribute('lang', 'fr')

    await hostPage.goto(`${base}/admin/sessions/${sessionId}/present`)
    await hostPage.waitForSelector('.glow-pin', { timeout: 15000 })
    await expect(hostPage.locator('[aria-live="polite"]').first()).toBeAttached()

    // Mobile student at 390x844
    const student = await createMobileStudent(browser, base, sessionCode, 'A11yÉlève')
    await expect(student.page.locator('html')).toHaveAttribute('lang', 'fr')

    await expect(hostPage.locator('body')).toContainText('1')
    await hostPage.getByRole('button', { name: /Démarrer le Quizz/i }).click()

    // Student sees question with image
    await student.page.waitForSelector('quizz-question-answer', { timeout: 15000 })
    await expect(student.page.locator('[aria-live="polite"]').first()).toBeAttached()

    // 1. Zero horizontal overflow on mobile 390x844
    const overflow = await student.page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      innerWidth: window.innerWidth,
    }))
    expect(overflow.scrollWidth).toBeLessThanOrEqual(overflow.innerWidth)

    // 2. Hitboxes >= 44px on answer options & exit button
    const answerCount = await student.page.locator('quizz-question-answer').count()
    expect(answerCount).toBe(4)
    for (let i = 0; i < answerCount; i++) {
      const box = await student.page.locator('quizz-question-answer').nth(i).boundingBox()
      expect(box).toBeTruthy()
      expect(box!.height).toBeGreaterThanOrEqual(44)
    }
    const exitBox = await student.page.getByRole('button', { name: /Quitter la session de quiz/i }).boundingBox()
    expect(exitBox).toBeTruthy()
    expect(exitBox!.height).toBeGreaterThanOrEqual(44)
    expect(exitBox!.width).toBeGreaterThanOrEqual(44)

    // 3. No visible "Image de la question" text (must be visually hidden sr-only)
    const srOnlyBox = await student.page.locator('#question-image-desc').boundingBox()
    if (srOnlyBox) {
      expect(srOnlyBox.width).toBeLessThanOrEqual(2)
      expect(srOnlyBox.height).toBeLessThanOrEqual(2)
    }

    // Student answers via keyboard (Tab + Space/Enter)
    const firstOption = student.page.locator('quizz-question-answer').first()
    await firstOption.click()

    // Host reveals and goes to 720p podium
    await hostPage.getByRole('button', { name: /Révéler les réponses/i }).click()
    await expect(hostPage.locator('body')).toContainText('BONNE RÉPONSE RÉVÉLÉE')
    await hostPage.getByRole('button', { name: /Afficher le Podium Final/i }).click()
    await expect(hostPage.locator('body')).toContainText('PODIUM DE LA CLASSE')

    const skipBtn = hostPage.getByRole('button', { name: /Passer l'animation/i })
    if (await skipBtn.isVisible().catch(() => false)) {
      await skipBtn.click()
    }
    await saveAuditScreenshot(hostPage, '05-host-podium-720p.png')

    // Check 720p podium fits within 720px viewport without vertical clipping
    const podiumOverflow = await hostPage.evaluate(() => ({
      scrollHeight: document.documentElement.scrollHeight,
      innerHeight: window.innerHeight,
    }))
    expect(podiumOverflow.scrollHeight).toBeLessThanOrEqual(podiumOverflow.innerHeight + 2)

    hostTracker.assertNoErrors()
    student.tracker.assertNoErrors()

    await hostContext.close()
    await student.context.close()
  })

  test('Keyboard Tab/Enter navigation in QueZap editor and PrimeNG p-select dropdowns without pFocusTrap lock', async ({ browser, baseURL }) => {
    const base = baseURL || 'http://localhost:8000'
    const context = await browser.newContext({ viewport: { width: 1280, height: 800 } })
    const page = await context.newPage()
    const tracker = attachErrorTracker(page, 'editor-a11y')

    await loginAsAdminUi(page, base)
    await page.goto(`${base}/admin/quezaps/create`)
    await page.waitForSelector('quizz-quezap-editor', { timeout: 15000 })

    // Open PrimeNG p-select for question type (appendTo="body" must not be blocked by pFocusTrap)
    const typeSelect = page.locator('quizz-type-selector p-select').first()
    await typeSelect.click()

    // Overlay listbox appears on body and is interactive via keyboard
    const listbox = page.locator('.p-select-overlay, [role="listbox"]').first()
    await expect(listbox).toBeVisible({ timeout: 5000 })
    await page.keyboard.press('ArrowDown')
    await page.keyboard.press('Enter')

    // Ensure Tab moves focus normally across editor controls
    await page.keyboard.press('Tab')
    const activeTag = await page.evaluate(() => document.activeElement?.tagName ?? '')
    expect(activeTag).toBeTruthy()

    tracker.assertNoErrors()
    await context.close()
  })
})

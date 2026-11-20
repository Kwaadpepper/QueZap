import fs from 'node:fs'

import { expect, test } from '@playwright/test'

import {
  apiCall,
  attachErrorTracker,
  loginAsAdminApi,
  loginAsAdminUi,
} from './helpers/quezap-e2e.helpers'

test.describe('04 - Session Report & Secure CSV Export', () => {
  test('End session -> /admin/sessions/:id/report -> real stats + BOM UTF-8 CSV + formula neutralization', async ({ browser, baseURL }) => {
    const base = baseURL || 'http://localhost:8000'
    const token = await loginAsAdminApi(base)

    // 1. Create QueZap & Session
    const quezapRes = await apiCall(base, '/quezaps', 'POST', {
      title: `E2E Report Quiz ${Date.now() % 100000}`,
      description: 'Test bilan et export CSV',
      isPublic: true,
      slides: [
        {
          timer: 20,
          timerSeconds: 20,
          points: 1000,
          question: {
            label: 'Combien font 2 + 2 ?',
            type: 'QUIZZ',
            answers: [
              { label: '4', isCorrect: true, answerOrder: 1 },
              { label: '5', isCorrect: false, answerOrder: 2 },
            ],
          },
        },
      ],
    }, token)
    expect(quezapRes.status).toBe(201)
    const quezapId = String(quezapRes.json?.['id'] || '')

    const sessionRes = await apiCall(base, '/sessions', 'POST', {
      name: `Session Rapport ${Date.now() % 10000}`,
      quezapId,
    }, token)
    expect(sessionRes.status).toBe(201)
    const sessionId = String(sessionRes.json?.['id'] || '')
    const sessionCode = String(sessionRes.json?.['code'] || '')

    // 2. Two participants join: one normal, one with leading formula character
    const p1 = await apiCall(base, '/sessions/join', 'POST', {
      code: sessionCode,
      participantName: 'Camille',
    })
    expect([200, 201]).toContain(p1.status)
    const p1Token = String(p1.json?.['participationToken'] || '')

    const p2 = await apiCall(base, '/sessions/join', 'POST', {
      code: sessionCode,
      participantName: '=SUM(A1:A2)',
    })
    expect([200, 201]).toContain(p2.status)
    const p2Token = String(p2.json?.['participationToken'] || '')

    // 3. Host starts, participants answer, host reveals & shows podium (ends session)
    const hostContext = await browser.newContext({ viewport: { width: 1920, height: 1080 }, acceptDownloads: true })
    const hostPage = await hostContext.newPage()
    const hostTracker = attachErrorTracker(hostPage, 'host-report')

    await loginAsAdminUi(hostPage, base)
    await hostPage.goto(`${base}/admin/sessions/${sessionId}/present`)
    await hostPage.waitForSelector('.glow-pin', { timeout: 15000 })
    await expect(hostPage.locator('body')).toContainText('2')

    await hostPage.getByRole('button', { name: /Démarrer le Quizz/i }).click()
    await expect(hostPage.locator('body')).toContainText('Question 1 / 1')

    await apiCall(base, `/sessions/${sessionId}/answers`, 'POST', { answerIndex: 0, slideIndex: 1 }, undefined, { 'X-Participant-Token': p1Token })
    await apiCall(base, `/sessions/${sessionId}/answers`, 'POST', { answerIndex: 1, slideIndex: 1 }, undefined, { 'X-Participant-Token': p2Token })

    await hostPage.getByRole('button', { name: /Révéler les réponses/i }).click()
    await expect(hostPage.locator('body')).toContainText('BONNE RÉPONSE RÉVÉLÉE')

    await hostPage.getByRole('button', { name: /Afficher le Podium Final/i }).click()
    await expect(hostPage.locator('body')).toContainText('PODIUM DE LA CLASSE')

    // 4. Navigate to /admin/sessions/:id/report
    await hostPage.goto(`${base}/admin/sessions/${sessionId}/report`)
    await expect(hostPage.locator('body')).toContainText('Session terminée', { timeout: 15000 })
    await expect(hostPage.locator('body')).toContainText('Classement des Élèves')
    await expect(hostPage.locator('body')).toContainText('Camille')
    await expect(hostPage.locator('body')).toContainText('50%')
    await expect(hostPage.locator('body')).toContainText('Combien font 2 + 2 ?')

    // 5. Download CSV and validate BOM, headers, and formula neutralization
    const downloadPromise = hostPage.waitForEvent('download')
    await hostPage.getByRole('button', { name: /Télécharger les résultats \(CSV\)/i }).click()
    const download = await downloadPromise
    expect(download.suggestedFilename()).toMatch(/^session-.*-resultats\.csv$/)

    const filePath = await download.path()
    expect(filePath).toBeTruthy()
    const rawBuffer = fs.readFileSync(filePath!)
    // UTF-8 BOM: EF BB BF
    expect(rawBuffer[0]).toBe(0xEF)
    expect(rawBuffer[1]).toBe(0xBB)
    expect(rawBuffer[2]).toBe(0xBF)

    const csvContent = rawBuffer.toString('utf8').replace(/^\uFEFF/, '')
    const lines = csvContent.trim().split(/\r?\n/)
    expect(lines.length).toBeGreaterThanOrEqual(3)

    // Expected headers
    expect(lines[0]).toMatch(/Rang.*Pseudo.*Score Total/i)

    // Formula neutralization: '=SUM(A1:A2) must be prefixed (e.g. "'=sum(a1:a2)") and never start unescaped with =, +, -, @
    const formulaLine = lines.find(l => l.toLowerCase().includes('sum(a1:a2)'))
    expect(formulaLine, `Formula line not found in CSV:\n${csvContent}`).toBeTruthy()
    expect(formulaLine?.toLowerCase()).toContain("'=sum(a1:a2)")

    hostTracker.assertNoErrors()
    await hostContext.close()
  })
})

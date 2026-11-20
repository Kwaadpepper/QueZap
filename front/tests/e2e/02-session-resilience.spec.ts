import { expect, test } from '@playwright/test'

import {
  apiCall,
  attachErrorTracker,
  createMobileStudent,
  loginAsAdminApi,
  loginAsAdminUi,
} from './helpers/quezap-e2e.helpers'

test.describe('02 - Session Resilience (F5/Reopen, Pause/Resume, Late Answer 409, Leave, Kick)', () => {
  test('Kick in lobby and voluntary leave', async ({ browser, baseURL }) => {
    const base = baseURL || 'http://localhost:8000'
    const token = await loginAsAdminApi(base)

    const quezapRes = await apiCall(base, '/quezaps', 'POST', {
      title: `E2E Resilience Lobby ${Date.now() % 100000}`,
      description: 'Test kick et leave',
      isPublic: true,
      slides: [
        {
          timer: 20,
          timerSeconds: 20,
          points: 1000,
          question: {
            label: 'Question 1',
            type: 'BOOLEAN',
            answers: [
              { label: 'Vrai', isCorrect: true, answerOrder: 1 },
              { label: 'Faux', isCorrect: false, answerOrder: 2 },
            ],
          },
        },
      ],
    }, token)
    expect(quezapRes.status).toBe(201)
    const quezapId = String(quezapRes.json?.['id'] || '')

    const sessionRes = await apiCall(base, '/sessions', 'POST', {
      name: `Resilience Lobby ${Date.now() % 10000}`,
      quezapId,
    }, token)
    expect(sessionRes.status).toBe(201)
    const sessionId = String(sessionRes.json?.['id'] || '')
    const sessionCode = String(sessionRes.json?.['code'] || '')

    const hostContext = await browser.newContext({ viewport: { width: 1920, height: 1080 } })
    const hostPage = await hostContext.newPage()
    const hostTracker = attachErrorTracker(hostPage, 'host-lobby')
    await loginAsAdminUi(hostPage, base)
    await hostPage.goto(`${base}/admin/sessions/${sessionId}/present`)
    await hostPage.waitForSelector('.glow-pin', { timeout: 15000 })

    const alice = await createMobileStudent(browser, base, sessionCode, 'Alice')
    const bob = await createMobileStudent(browser, base, sessionCode, 'Bob')

    await expect(hostPage.locator('body')).toContainText('2')

    // Host kicks Bob
    const kickBtn = hostPage.getByRole('button', { name: 'Exclure le joueur Bob' })
    await kickBtn.click()
    await hostPage.getByRole('button', { name: /Confirmer l'exclusion/i }).click()

    // Bob is redirected to /quizz/ended
    await bob.page.waitForURL(/\/quizz\/ended/, { timeout: 15000 })
    await expect(hostPage.locator('body')).toContainText('1 joueur(s) dans le salon')

    // Alice leaves voluntarily via exit button
    await alice.page.getByRole('button', { name: /Quitter la session de quiz/i }).click()
    await alice.page.getByRole('button', { name: 'Quitter', exact: true }).click()
    await alice.page.waitForURL(/\/quizz\/ended|\/quizz/, { timeout: 15000 })

    await expect(hostPage.locator('body')).toContainText('0 joueur(s) dans le salon')

    hostTracker.assertNoErrors()
    alice.tracker.assertNoErrors()
    bob.tracker.assertNoErrors()

    await hostContext.close()
    await alice.context.close()
    await bob.context.close()
  })

  test('F5 refresh, tab close/reopen via localStorage, Pause/Resume, and 409 late answer', async ({ browser, baseURL }) => {
    const base = baseURL || 'http://localhost:8000'
    const token = await loginAsAdminApi(base)

    const quezapRes = await apiCall(base, '/quezaps', 'POST', {
      title: `E2E Resilience Live ${Date.now() % 100000}`,
      description: 'Test F5, pause, 409',
      isPublic: true,
      slides: [
        {
          timer: 30,
          timerSeconds: 30,
          points: 1000,
          question: {
            label: 'Quel langage est utilisé pour Angular ?',
            type: 'QUIZZ',
            answers: [
              { label: 'TypeScript', isCorrect: true, answerOrder: 1 },
              { label: 'Ruby', isCorrect: false, answerOrder: 2 },
              { label: 'COBOL', isCorrect: false, answerOrder: 3 },
              { label: 'PHP', isCorrect: false, answerOrder: 4 },
            ],
          },
        },
        {
          timer: 25,
          timerSeconds: 25,
          points: 1000,
          question: {
            label: 'Question 2 pour test pause',
            type: 'BOOLEAN',
            answers: [
              { label: 'Vrai', isCorrect: true, answerOrder: 1 },
              { label: 'Faux', isCorrect: false, answerOrder: 2 },
            ],
          },
        },
      ],
    }, token)
    expect(quezapRes.status).toBe(201)
    const quezapId = String(quezapRes.json?.['id'] || '')

    const sessionRes = await apiCall(base, '/sessions', 'POST', {
      name: `Resilience Live ${Date.now() % 10000}`,
      quezapId,
    }, token)
    expect(sessionRes.status).toBe(201)
    const sessionId = String(sessionRes.json?.['id'] || '')
    const sessionCode = String(sessionRes.json?.['code'] || '')

    const hostContext = await browser.newContext({ viewport: { width: 1920, height: 1080 } })
    const hostPage = await hostContext.newPage()
    const hostTracker = attachErrorTracker(hostPage, 'host-live')
    await loginAsAdminUi(hostPage, base)
    await hostPage.goto(`${base}/admin/sessions/${sessionId}/present`)
    await hostPage.waitForSelector('.glow-pin', { timeout: 15000 })

    // Student 1 joins in browser, Bot joins via API
    const clara = await createMobileStudent(browser, base, sessionCode, 'Clara')
    const botJoin = await apiCall(base, '/sessions/join', 'POST', {
      code: sessionCode,
      participantName: 'LateBot',
    })
    expect([200, 201]).toContain(botJoin.status)
    const botToken = String(botJoin.json?.['participationToken'] || '')

    await expect(hostPage.locator('body')).toContainText('2')
    await hostPage.getByRole('button', { name: /Démarrer le Quizz/i }).click()

    // Clara votes on Question 1
    await clara.page.locator('quizz-question-answer').nth(0).waitFor({ state: 'visible', timeout: 15000 })
    await clara.page.locator('quizz-question-answer').nth(0).click()
    await expect(clara.page.locator('body')).toContainText(/Choix enregistré|enregistré/i)

    // 1. F5 Refresh on Clara's page -> restores session & submitted choice
    await clara.page.reload()
    await clara.page.waitForSelector('quizz-question-answer', { timeout: 15000 })
    await expect(clara.page.locator('body')).toContainText(/Choix enregistré|enregistré/i)

    // 2. Close tab and reopen a new page in the same context (localStorage persistence)
    const currentUrl = clara.page.url()
    await clara.page.close()
    const reopenedPage = await clara.context.newPage()
    const reopenedTracker = attachErrorTracker(reopenedPage, 'student:Clara-reopened')
    await reopenedPage.goto(currentUrl)
    await reopenedPage.waitForSelector('quizz-question-answer', { timeout: 15000 })
    await expect(reopenedPage.locator('body')).toContainText(/Choix enregistré|enregistré/i)

    // 3. Host reveals Question 1 -> LateBot attempts late answer -> 409 Conflict
    await hostPage.getByRole('button', { name: /Révéler les réponses/i }).click()
    await expect(hostPage.locator('body')).toContainText('BONNE RÉPONSE RÉVÉLÉE')

    const lateRes = await apiCall(base, `/sessions/${sessionId}/answers`, 'POST', {
      answerIndex: 0,
      slideIndex: 1,
    }, undefined, { 'X-Participant-Token': botToken })
    expect([400, 409]).toContain(lateRes.status)

    // 4. Question 2 -> Pause & Resume
    await hostPage.getByRole('button', { name: /Question suivante/i }).click()
    await expect(hostPage.locator('body')).toContainText('Question 2 / 2')
    await reopenedPage.locator('quizz-question-answer').nth(0).waitFor({ state: 'visible', timeout: 15000 })

    // Host pauses
    await hostPage.getByRole('button', { name: /Mettre en pause/i }).click()
    await expect(hostPage.locator('body')).toContainText('EN PAUSE')
    await expect(reopenedPage.locator('body')).toContainText('EN PAUSE')

    // Student cannot vote while paused (fieldset is disabled)
    await expect(reopenedPage.locator('form[role="radiogroup"]')).toHaveAttribute('inert', '')

    // Host resumes
    await hostPage.getByRole('button', { name: /Reprendre/i }).click()
    await expect(hostPage.locator('body')).not.toContainText('EN PAUSE')
    await expect(reopenedPage.locator('body')).not.toContainText('EN PAUSE')

    // Now Clara can vote
    await reopenedPage.locator('quizz-question-answer').nth(0).click()
    await expect(reopenedPage.locator('body')).toContainText(/Choix enregistré|enregistré/i)

    hostTracker.assertNoErrors()
    clara.tracker.assertNoErrors()
    reopenedTracker.assertNoErrors()

    await hostContext.close()
    await clara.context.close()
  })
})

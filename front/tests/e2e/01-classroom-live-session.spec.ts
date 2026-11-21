import { expect, test } from '@playwright/test'

import {
  apiCall,
  attachErrorTracker,
  expectPodiumLayoutClean,
  createMobileStudent,
  loginAsAdminApi,
  loginAsAdminUi,
  saveAuditScreenshot,
} from './helpers/quezap-e2e.helpers'

test.describe('01 - Classroom Live Session (Multi-Types: Vrai/Faux, QCM, Cash + 35 participants)', () => {
  test('Full live session from lobby to final podium and student /quizz/ended', async ({ browser, baseURL }) => {
    test.setTimeout(120000)
    const base = baseURL || 'http://localhost:8000'
    const token = await loginAsAdminApi(base)

    // 1. Create a complete QueZap with BOOLEAN, QUIZZ, and CASH questions
    const quezapRes = await apiCall(base, '/quezaps', 'POST', {
      title: `E2E Live Multi-Type ${Date.now() % 100000}`,
      description: 'Quiz complet Vrai/Faux, QCM et Cash',
      isPublic: true,
      slides: [
        {
          timer: 25,
          timerSeconds: 25,
          points: 1000,
          question: {
            label: 'La Terre tourne autour du Soleil ?',
            type: 'BOOLEAN',
            answers: [
              { label: 'Vrai', isCorrect: true, answerOrder: 1 },
              { label: 'Faux', isCorrect: false, answerOrder: 2 },
            ],
          },
        },
        {
          timer: 25,
          timerSeconds: 25,
          points: 1000,
          question: {
            label: 'Quelle est la capitale de la France ?',
            type: 'QUIZZ',
            answers: [
              { label: 'Paris', isCorrect: true, answerOrder: 1 },
              { label: 'Lyon', isCorrect: false, answerOrder: 2 },
              { label: 'Marseille', isCorrect: false, answerOrder: 3 },
              { label: 'Toulouse', isCorrect: false, answerOrder: 4 },
            ],
          },
        },
        {
          timer: 30,
          timerSeconds: 30,
          points: 1500,
          question: {
            label: 'Quel est le plus grand mammifère terrestre ?',
            type: 'CASH',
            answers: [
              { label: 'Éléphant', isCorrect: true, answerOrder: 1 },
            ],
          },
        },
      ],
    }, token)
    expect(quezapRes.status, `Quezap creation failed: ${quezapRes.text}`).toBe(201)
    const quezapId = String(quezapRes.json?.['id'] || '')
    expect(quezapId).toBeTruthy()

    const sessionRes = await apiCall(base, '/sessions', 'POST', {
      name: `Classe E2E Live ${Date.now() % 10000}`,
      quezapId,
    }, token)
    expect(sessionRes.status).toBe(201)
    const sessionId = String(sessionRes.json?.['id'] || '')
    const sessionCode = String(sessionRes.json?.['code'] || '')
    expect(sessionId).toBeTruthy()
    expect(sessionCode).toBeTruthy()

    // 2. Host context (1920x1080)
    const hostContext = await browser.newContext({ viewport: { width: 1920, height: 1080 } })
    const hostPage = await hostContext.newPage()
    const hostTracker = attachErrorTracker(hostPage, 'host')

    await loginAsAdminUi(hostPage, base)
    await hostPage.goto(`${base}/admin/sessions/${sessionId}/present`)
    await hostPage.waitForSelector('.glow-pin', { timeout: 15000 })

    // 3. Three mobile students (390x844)
    const student1 = await createMobileStudent(browser, base, sessionCode, 'Léa')
    const student2 = await createMobileStudent(browser, base, sessionCode, 'Maximilien')
    const student3 = await createMobileStudent(browser, base, sessionCode, 'Zoé')
    const students = [student1, student2, student3]

    // 4. 32 HTTP bots join concurrently
    const botTokens: string[] = []
    for (let i = 1; i <= 32; i++) {
      const botJoin = await apiCall(base, '/sessions/join', 'POST', {
        code: sessionCode,
        participantName: `Bot_${i}`,
      })
      expect([200, 201]).toContain(botJoin.status)
      botTokens.push(String(botJoin.json?.['participationToken'] || ''))
    }

    // Host sees 35 participants
    await expect(hostPage.locator('body')).toContainText('35')
    await saveAuditScreenshot(hostPage, '01-host-lobby.png')

    // 5. Start quiz -> Question 1 (BOOLEAN)
    await hostPage.getByRole('button', { name: /Démarrer le Quizz/i }).click()
    await expect(hostPage.locator('body')).toContainText('Question 1 / 3')

    // Students vote on Question 1 (Léa & Maximilien: Vrai, Zoé: Faux)
    await student1.page.locator('quizz-question-answer').nth(0).click()
    await student2.page.locator('quizz-question-answer').nth(0).click()
    await student3.page.locator('quizz-question-answer').nth(1).click()

    await expect(student1.page.locator('body')).toContainText(/Choix enregistré|enregistré/i)

    // 32 bots answer Question 1 concurrently
    const t0 = Date.now()
    const botAnswersQ1 = await Promise.all(
      botTokens.map((tok, idx) =>
        apiCall(base, `/sessions/${sessionId}/answers`, 'POST', {
          answerIndex: idx % 2 === 0 ? 0 : 1,
          slideIndex: 1,
        }, undefined, { 'X-Participant-Token': tok }),
      ),
    )
    expect(Date.now() - t0).toBeLessThan(8000)
    for (const res of botAnswersQ1) {
      expect([200, 201, 204]).toContain(res.status)
    }

    await expect(hostPage.locator('body')).toContainText('35 / 35')
    await hostPage.getByRole('button', { name: /Révéler les réponses/i }).click()
    await expect(hostPage.locator('body')).toContainText('BONNE RÉPONSE RÉVÉLÉE')

    // Intermediate Leaderboard
    await hostPage.getByRole('button', { name: /Classement/i }).click()
    await expect(hostPage.locator('body')).toContainText('CLASSEMENT INTERMÉDIAIRE')
    await expect(hostPage.locator('body')).toContainText('Top 5 de la Classe')

    // 6. Question 2 (QUIZZ - 4 choices)
    await hostPage.getByRole('button', { name: /Question suivante/i }).click()
    await expect(hostPage.locator('body')).toContainText('Question 2 / 3')

    await student1.page.locator('quizz-question-answer').nth(0).waitFor({ state: 'visible', timeout: 15000 })
    await saveAuditScreenshot(student1.page, '01-student-mobile-question.png')

    await student1.page.locator('quizz-question-answer').nth(0).click()
    await student2.page.locator('quizz-question-answer').nth(0).click()
    await student3.page.locator('quizz-question-answer').nth(2).click()

    await Promise.all(
      botTokens.map(tok =>
        apiCall(base, `/sessions/${sessionId}/answers`, 'POST', {
          answerIndex: 1,
          slideIndex: 2,
        }, undefined, { 'X-Participant-Token': tok }),
      ),
    )

    await hostPage.getByRole('button', { name: /Révéler les réponses/i }).click()
    await expect(hostPage.locator('body')).toContainText('BONNE RÉPONSE RÉVÉLÉE')

    // 7. Question 3 (CASH - tolerance on case, accents, doubled letters)
    await hostPage.getByRole('button', { name: /Question suivante/i }).click()
    await expect(hostPage.locator('body')).toContainText('Question 3 / 3')
    await expect(hostPage.locator('body')).toContainText('Mode Cash')

    // Student 1: exact ("Éléphant")
    await student1.page.locator('#cash-input').waitFor({ state: 'visible', timeout: 15000 })
    await student1.page.locator('#cash-input').fill('Éléphant')
    await student1.page.locator('button[type="submit"]').click()

    // Student 2: lowercase, no accent, doubled letter ("elepphant")
    await student2.page.locator('#cash-input').waitFor({ state: 'visible', timeout: 15000 })
    await student2.page.locator('#cash-input').fill('elepphant')
    await student2.page.locator('button[type="submit"]').click()

    // Student 3: wrong answer ("Girafe")
    await student3.page.locator('#cash-input').waitFor({ state: 'visible', timeout: 15000 })
    await student3.page.locator('#cash-input').fill('Girafe')
    await student3.page.locator('button[type="submit"]').click()

    // Bots answer wrong so Léa and Maximilien are guaranteed top 2
    await Promise.all(
      botTokens.map(tok =>
        apiCall(base, `/sessions/${sessionId}/answers`, 'POST', {
          textAnswer: 'Rhinocéros',
          slideIndex: 3,
        }, undefined, { 'X-Participant-Token': tok }),
      ),
    )

    await hostPage.getByRole('button', { name: /Révéler les réponses/i }).click()
    await expect(hostPage.locator('body')).toContainText('BONNE RÉPONSE RÉVÉLÉE')
    await expect(hostPage.locator('body')).toContainText('Éléphant')

    // 8. Final Podium
    await hostPage.getByRole('button', { name: /Afficher le Podium Final/i }).click()
    await expect(hostPage.locator('body')).toContainText('PODIUM DE LA CLASSE')

    const skipBtn = hostPage.getByRole('button', { name: /Passer l'animation/i })
    if (await skipBtn.isVisible().catch(() => false)) {
      await skipBtn.click()
    }
    await expectPodiumLayoutClean(hostPage)
    await saveAuditScreenshot(hostPage, '01-host-podium-1080p.png')

    // 9. Students redirected to /quizz/ended with rank and score
    for (const s of students) {
      await s.page.waitForURL(/\/quizz\/ended/, { timeout: 15000 })
      await expect(s.page.locator('body')).toContainText('Classement')
      await expect(s.page.locator('body')).toContainText('Score')
    }
    // Léa and Maximilien answered all 3 correctly (including Cash with tolerance) -> rank #1 or #2
    await expect(student1.page.locator('body')).toContainText(/#[12]/)
    await expect(student2.page.locator('body')).toContainText(/#[12]/)

    // 10. Assert 0 console errors & 0 uncaught page errors
    hostTracker.assertNoErrors()
    for (const s of students) {
      s.tracker.assertNoErrors()
    }

    await hostContext.close()
    for (const s of students) {
      await s.context.close()
    }
  })
})

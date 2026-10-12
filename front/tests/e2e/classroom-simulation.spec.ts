import { expect, test } from '@playwright/test'

test.describe('Classroom Quiz Simulation', () => {
  test('Full quiz simulation with 1 host screen, 3 mobile students and 32 HTTP bots', async ({ browser, baseURL }) => {
    const base = baseURL || 'http://localhost:8000'
    const apiBase = `${base}/apiv1`

    // Helper for API calls
    const api = async (path: string, method = 'GET', body?: unknown, token?: string, headers: Record<string, string> = {}) => {
      const res = await fetch(`${apiBase}${path}`, {
        method,
        headers: {
          'content-type': 'application/json',
          ...(token ? { authorization: `Bearer ${token}` } : {}),
          ...headers,
        },
        body: body ? JSON.stringify(body) : undefined,
      })
      let json: Record<string, unknown> | null = null
      try {
        json = (await res.json()) as Record<string, unknown>
      }
      catch {}
      return { status: res.status, json }
    }

    // 1. Host logs in and creates a session
    const loginRes = await api('/auth/login', 'POST', { identifier: 'admin@quezap.com', password: 'Password123!' })
    expect(loginRes.status).toBe(200)
    const token = String(loginRes.json?.['accessToken'] || '')
    expect(token).toBeTruthy()

    const quezapsRes = await api('/quezaps?size=50', 'GET', undefined, token)
    expect(quezapsRes.status).toBe(200)
    const quezapsList = (quezapsRes.json?.['data'] as Array<{ id: string; totalQuestions?: number }>) || []
    const quezap = quezapsList.find(q => (q.totalQuestions ?? 0) >= 1) || quezapsList[0]
    expect(quezap).toBeTruthy()

    const sessionRes = await api('/sessions', 'POST', { name: `E2E Class ${Date.now() % 10000}`, quezapId: quezap.id }, token)
    expect(sessionRes.status).toBe(201)
    const sessionId = String(sessionRes.json?.['id'] || '')
    const sessionCode = String(sessionRes.json?.['code'] || '')
    expect(sessionId).toBeTruthy()
    expect(sessionCode).toBeTruthy()

    // 2. Host browser context
    const hostContext = await browser.newContext({ viewport: { width: 1920, height: 1080 } })
    const hostPage = await hostContext.newPage()

    await hostPage.goto(`${base}/auth/login`)
    await hostPage.locator('input[type="password"]').first().fill('Password123!')
    await hostPage.locator('input').first().fill('admin@quezap.com')
    await hostPage.keyboard.press('Enter')
    await hostPage.waitForURL(/\/admin/, { timeout: 15000 })

    await hostPage.goto(`${base}/admin/sessions/${sessionId}/present`)
    await hostPage.waitForSelector('.glow-pin, p:has-text("Code PIN")', { timeout: 15000 })

    // 3. Three mobile student browsers
    const studentNames = ['Léa', 'Maximilien', 'Zoé']
    const studentPages = []

    for (const name of studentNames) {
      const studentContext = await browser.newContext({
        viewport: { width: 390, height: 844 },
        isMobile: true,
        hasTouch: true,
      })
      const studentPage = await studentContext.newPage()
      await studentPage.goto(`${base}/quizz/join/${sessionCode}`)
      await studentPage.locator('#nickname-value').fill(name)
      await studentPage.locator('button[type="submit"]').click()
      await studentPage.waitForSelector('text=En attente', { timeout: 15000 })
      studentPages.push(studentPage)
    }

    // 4. 32 bots join via HTTP
    const botTokens: string[] = []
    for (let i = 1; i <= 32; i++) {
      const botJoin = await api('/sessions/join', 'POST', { code: sessionCode, participantName: `Bot_${i}` })
      expect([200, 201]).toContain(botJoin.status)
      botTokens.push(String(botJoin.json?.['participationToken'] || ''))
    }

    // Assert lobby count in host DOM
    await expect(hostPage.locator('body')).toContainText('35')

    // 5. Host starts the quiz
    const startButton = hostPage.getByRole('button', { name: /Démarrer le Quizz/i })
    await startButton.click()

    // Students see question and vote
    await studentPages[0].waitForSelector('quizz-question-answer', { timeout: 15000 })
    await studentPages[0].locator('quizz-question-answer').first().click()

    // 32 bots answer concurrently
    const t0 = Date.now()
    const botAnswers = await Promise.all(
      botTokens.map(tok =>
        api(`/sessions/${sessionId}/answers`, 'POST', { answerIndex: 0, slideIndex: 1 }, undefined, { 'X-Participant-Token': tok }),
      ),
    )
    const durationMs = Date.now() - t0
    expect(durationMs).toBeLessThan(5000)
    for (const res of botAnswers) {
      expect([200, 201, 204]).toContain(res.status)
    }

    // 6. Host reveals answers
    const revealBtn = hostPage.getByRole('button', { name: /Révéler/i })
    await revealBtn.click()
    await expect(hostPage.locator('body')).toContainText('BONNE RÉPONSE RÉVÉLÉE')

    // 7. Intermediate Leaderboard
    const leaderboardBtn = hostPage.getByRole('button', { name: /Voir le Classement/i })
    await leaderboardBtn.waitFor({ state: 'visible', timeout: 5000 })
    await leaderboardBtn.click()
    await expect(hostPage.locator('body')).toContainText('CLASSEMENT INTERMÉDIAIRE')
    await expect(hostPage.locator('body')).toContainText('Top 5 de la Classe')

    // 8. Progress through remaining questions if any
    const totalQuestions = Number(quezap.totalQuestions ?? 1)
    for (let q = 2; q <= totalQuestions; q++) {
      const nextBtn = hostPage.getByRole('button', { name: /Question suivante/i })
      await nextBtn.click()
      await hostPage.waitForSelector('.choice-grid', { timeout: 10000 })

      // Students & bots vote on slide q
      await Promise.all(
        botTokens.map(tok =>
          api(`/sessions/${sessionId}/answers`, 'POST', { answerIndex: 0, slideIndex: q }, undefined, { 'X-Participant-Token': tok }),
        ),
      )

      // Host reveals
      const rev = hostPage.getByRole('button', { name: /Révéler/i })
      await rev.click()
      await expect(hostPage.locator('body')).toContainText('BONNE RÉPONSE RÉVÉLÉE')
    }

    // 9. Final Podium of the classroom
    const podiumBtn = hostPage.getByRole('button', { name: /Afficher le Podium Final/i })
    await podiumBtn.click()
    await expect(hostPage.locator('body')).toContainText('PODIUM DE LA CLASSE')

    await hostContext.close()
    for (const sp of studentPages) {
      await sp.context().close()
    }
  })
})

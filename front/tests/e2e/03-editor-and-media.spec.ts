import { expect, test } from '@playwright/test'

import {
  attachErrorTracker,
  createMobileStudent,
  createValidPngBuffer,
  loginAsAdminUi,
  saveAuditScreenshot,
} from './helpers/quezap-e2e.helpers'

test.describe('03 - QueZap Editor & S3 Media Upload', () => {
  test('Create QueZap, reject invalid upload, upload valid PNG, save, launch live, and render image on Host & Mobile', async ({ browser, baseURL }) => {
    test.setTimeout(120000)
    const base = baseURL || 'http://localhost:8000'

    const hostContext = await browser.newContext({ viewport: { width: 1920, height: 1080 } })
    const hostPage = await hostContext.newPage()
    const hostTracker = attachErrorTracker(hostPage, 'host-editor')

    await loginAsAdminUi(hostPage, base)
    await hostPage.goto(`${base}/admin/quezaps/create`)
    await hostPage.waitForSelector('quizz-quezap-editor', { timeout: 15000 })

    // 1. Edit QueZap title
    await hostPage.locator('quizz-title-editor .p-inplace-display').click()
    const titleInput = hostPage.locator('quizz-title-editor input')
    await titleInput.fill(`Quiz Média E2E ${Date.now() % 100000}`)
    await titleInput.press('Enter')

    // 2. Edit Question 1 phrase
    await hostPage.locator('quizz-phrase-editor .p-inplace-display').click()
    const phraseInput = hostPage.locator('quizz-phrase-editor input')
    await phraseInput.fill('Quel est cet animal sur la photo ?')
    await phraseInput.press('Enter')

    // 3. Fill Answer 1 & 2 and mark Answer 1 as correct
    const answerInputs = hostPage.locator('quizz-answer quizz-phrase-input textarea')
    await answerInputs.nth(0).fill('Chat')

    await answerInputs.nth(1).fill('Chien')

    // Mark first answer as correct
    await hostPage.locator('quizz-answer quizz-answer-correctness input[type="checkbox"]').first().click()

    // 4. Reject invalid file (fake PNG / text payload)
    const questionFileInput = hostPage.locator('quizz-question-editor > div > main p-fileupload input[type="file"]').first()
    const invalidUploadPromise = hostPage.waitForResponse(
      res => res.url().includes('/apiv1/pictures/upload') && res.request().method() === 'POST',
    )
    await questionFileInput.setInputFiles({
      name: 'fake-image.png',
      mimeType: 'image/png',
      buffer: Buffer.from('NOT_A_REAL_PNG_FILE_CONTENT'),
    })
    const invalidRes = await invalidUploadPromise
    expect(invalidRes.status()).toBeGreaterThanOrEqual(400)
    await expect(hostPage.locator('p-toast')).toBeVisible({ timeout: 10000 })

    // 5. Upload valid PNG image
    const validUploadPromise = hostPage.waitForResponse(
      res => res.url().includes('/apiv1/pictures/upload') && res.request().method() === 'POST',
    )
    await questionFileInput.setInputFiles({
      name: 'test-picture.png',
      mimeType: 'image/png',
      buffer: createValidPngBuffer(),
    })
    const validRes = await validUploadPromise
    expect([200, 201]).toContain(validRes.status())
    const uploadJson = (await validRes.json()) as { pictureUrl?: string }
    expect(uploadJson.pictureUrl).toContain('/quezap-public-images/')

    // Preview image is displayed in editor
    const editorImg = hostPage.locator('quizz-question-editor img[alt="Question Image"]')
    await expect(editorImg).toBeVisible({ timeout: 10000 })
    const editorImgLoaded = await editorImg.evaluate((img: HTMLImageElement) => img.complete && img.naturalWidth > 0)
    expect(editorImgLoaded).toBe(true)
    await saveAuditScreenshot(hostPage, '03-editor-with-picture.png')

    // 6. Save QueZap
    const savePromise = hostPage.waitForResponse(
      res => res.url().includes('/apiv1/quezaps') && res.request().method() === 'POST',
    )
    await hostPage.getByRole('button', { name: 'Enregistrer' }).click()
    const saveRes = await savePromise
    expect([200, 201]).toContain(saveRes.status())

    // 7. Launch live session directly from editor (opens presenter in new tab)
    const [presenterPage] = await Promise.all([
      hostContext.waitForEvent('page'),
      hostPage.getByRole('button', { name: /Lancer en direct/i }).click(),
    ])
    const presenterTracker = attachErrorTracker(presenterPage, 'host-presenter')
    await presenterPage.waitForURL(/\/admin\/sessions\/.*\/present/, { timeout: 15000 })
    await presenterPage.waitForSelector('.glow-pin', { timeout: 15000 })
    const pin = (await presenterPage.locator('.glow-pin').innerText()).trim()
    expect(pin).toBeTruthy()

    // 8. Mobile student joins & host starts quiz
    const student = await createMobileStudent(browser, base, pin, 'MédiaÉlève')
    await expect(presenterPage.locator('body')).toContainText('1')
    await presenterPage.getByRole('button', { name: /Démarrer le Quizz/i }).click()

    // 9. Image renders on Host (QUESTION) and Mobile Student
    const hostQuestionImg = presenterPage.locator('p-image img[alt="Illustration de la question"]')
    await expect(hostQuestionImg).toBeVisible({ timeout: 15000 })
    await expect(hostQuestionImg).toHaveJSProperty('complete', true)
    const hostImgWidth = await hostQuestionImg.evaluate((img: HTMLImageElement) => img.naturalWidth)
    expect(hostImgWidth).toBeGreaterThan(0)
    await saveAuditScreenshot(presenterPage, '03-host-question-with-image.png')

    const studentImg = student.page.locator('quizz-picture img')
    await expect(studentImg).toBeVisible({ timeout: 15000 })
    await expect(studentImg).toHaveJSProperty('complete', true)
    const studentImgWidth = await studentImg.evaluate((img: HTMLImageElement) => img.naturalWidth)
    expect(studentImgWidth).toBeGreaterThan(0)

    // Student votes, host reveals -> image also visible on REVEAL
    await student.page.locator('quizz-question-answer').first().click()
    await presenterPage.getByRole('button', { name: /Révéler les réponses/i }).click()
    await expect(presenterPage.locator('body')).toContainText('BONNE RÉPONSE RÉVÉLÉE')
    await expect(presenterPage.locator('p-image img[alt="Illustration de la question"]')).toBeVisible()
    await saveAuditScreenshot(presenterPage, '03-host-reveal-with-image.png')

    // Expect no uncaught pageerrors; allow the expected 4xx resource console error from invalid picture upload
    hostTracker.assertNoErrors(['/apiv1/pictures/upload', '400', '415', '422'])
    presenterTracker.assertNoErrors()
    student.tracker.assertNoErrors()

    await student.context.close()
    await hostContext.close()
  })
})

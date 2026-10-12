const { chromium } = require('/tmp/bunx-1000-playwright@latest/node_modules/playwright-core');
(async () => {
  const code = process.argv[2];
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true });
  const p = await ctx.newPage();
  p.setDefaultTimeout(5000);
  p.on('console', m => console.log('CONSOLE', m.type(), m.text().slice(0, 200)));
  await p.goto('http://localhost:8000/quizz/join/' + code);
  console.log('inputs', await p.locator('input').count());
  for (const h of await p.locator('input').all()) console.log(await h.evaluate(e => e.outerHTML.slice(0, 200)));
  await p.locator('#nickname-value').fill('Léa').catch(e => console.log('FILLERR', e.message.split('\n')[0]));
  console.log('val', await p.locator('#nickname-value').inputValue().catch(e => 'ERR'));
  await p.locator('button[type=submit]').click().catch(e => console.log('CLICKERR', e.message.split('\n')[0]));
  await p.waitForTimeout(2000);
  console.log(p.url(), (await p.locator('body').innerText()).replace(/\n/g, ' | ').slice(0, 300));
  await browser.close();
})();

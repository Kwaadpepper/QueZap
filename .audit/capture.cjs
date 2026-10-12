const { chromium } = require('/tmp/bunx-1000-playwright@latest/node_modules/playwright-core');
const BASE = 'http://localhost:8000';
const OUT = '/home/jeremy/projets/QueZap/.audit/screenshots/';
const log = [];
async function shot(page, name) {
  await page.waitForTimeout(1200);
  await page.screenshot({ path: OUT + name + '.png', fullPage: true });
  log.push(name + ' <- ' + page.url());
}
(async () => {
  const browser = await chromium.launch();
  // Host desktop
  const host = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const p = await host.newPage();
  await p.goto(BASE + '/'); await shot(p, 'host_00_landing');
  await p.goto(BASE + '/auth/login'); await shot(p, 'host_01_login');
  await p.goto(BASE + '/auth/reset'); await shot(p, 'host_02_reset');
  await p.goto(BASE + '/about'); await shot(p, 'host_03_about');
  await p.goto(BASE + '/auth/login');
  try {
    await p.locator('input[type="email"], input[name="email"], input[formcontrolname="email"]').first().fill('admin@quezap.com');
    await p.locator('input[type="password"]').first().fill('Password123!');
    await p.keyboard.press('Enter');
    await p.waitForURL(/admin/, { timeout: 8000 });
  } catch (e) { log.push('LOGIN FAILED ' + e.message.split('\n')[0]); }
  const pages = ['dashboard', 'sessions', 'sessions/create', 'quezaps', 'quezaps/create', 'themes'];
  let i = 10;
  for (const r of pages) {
    await p.goto(BASE + '/admin/' + r);
    await shot(p, `host_${i++}_admin_${r.replace('/', '_')}`);
  }
  // Player mobile
  const mob = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true });
  const m = await mob.newPage();
  for (const [n, r] of [['join', '/quizz/join'], ['join_code', '/quizz/join/ABC123'], ['expired', '/quizz/expired'], ['ended', '/quizz/ended'], ['lobby', '/quizz/lobby']]) {
    await m.goto(BASE + r); await shot(m, `player_${n}`);
  }
  require('fs').writeFileSync(OUT + 'INDEX.txt', log.join('\n'));
  console.log(log.join('\n'));
  await browser.close();
})();

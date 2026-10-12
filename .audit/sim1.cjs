// Phase-based classroom simulation. Usage: node sim.cjs
const { chromium } = require('/tmp/bunx-1000-playwright@latest/node_modules/playwright-core');
const fs = require('fs');
const BASE = 'http://localhost:8000', B = BASE + '/apiv1';
const OUT = '/home/jeremy/projets/QueZap/.audit/screenshots/';
const log = [];
const L = (...a) => { const s = a.join(' '); log.push(s); console.log(s); };
const shot = async (page, name, full = false) => { await page.waitForTimeout(900); await page.screenshot({ path: OUT + name + '.png', fullPage: full }); L('SHOT', name, page.url()); };
const api = async (path, method = 'GET', body, tok, extra = {}) => {
  const r = await fetch(B + path, { method, headers: { 'content-type': 'application/json', ...(tok ? { authorization: 'Bearer ' + tok } : {}), ...extra }, body: body ? JSON.stringify(body) : undefined });
  let j = null; try { j = await r.json(); } catch {}
  return { s: r.status, j };
};
(async () => {
  const lg = await api('/auth/login', 'POST', { identifier: 'admin@quezap.com', password: 'Password123!' });
  const tok = lg.j.accessToken;
  const ql = await api('/quezaps?size=50', 'GET', null, tok);
  const qz = ql.j.data.find(x => x.totalQuestions >= 3);
  const full = await api('/quezaps/' + qz.id, 'GET', null, tok);
  L('QUEZAP', JSON.stringify(full.j).slice(0, 1500));
  const ses = await api('/sessions', 'POST', { name: 'Audit Live ' + Date.now() % 1000, quezapId: qz.id }, tok);
  L('SESSION', ses.s, JSON.stringify(ses.j));
  fs.writeFileSync('/tmp/audit_session.json', JSON.stringify({ ...ses.j, tok }));
  const browser = await chromium.launch();
  const host = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const hp = await host.newPage();
  hp.on('console', m => { if (m.type() === 'error') L('HOSTERR', m.text().slice(0, 200)); });
  await hp.goto(BASE + '/auth/login');
  await hp.locator('input[type="password"]').first().fill('Password123!');
  await hp.locator('input').first().fill('admin@quezap.com');
  await hp.keyboard.press('Enter');
  await hp.waitForURL(/admin/, { timeout: 10000 }).catch(e => L('LOGINFAIL', e.message));
  await hp.goto(BASE + '/admin/sessions/' + ses.j.id + '/present');
  await shot(hp, 'host_20_lobby_empty');
  const m = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true });
  const p = await m.newPage();
  await p.goto(BASE + '/quizz/join/' + ses.j.code);
  await shot(p, 'player_01_join_prefilled');
  L('PLAYER HTML', (await p.locator('body').innerText()).slice(0, 500));
  L('BUTTONS', JSON.stringify(await p.locator('button').allInnerTexts()));
  fs.writeFileSync('/tmp/sim.log', log.join('\n'));
  await browser.close();
})();

const { chromium } = require('/tmp/bunx-1000-playwright@latest/node_modules/playwright-core');
const fs = require('fs');
const BASE = 'http://localhost:8000', B = BASE + '/apiv1';
const OUT = '/home/jeremy/projets/QueZap/.audit/screenshots/';
const log = [];
const L = (...a) => { const s = a.join(' '); log.push(s); console.log(s); fs.writeFileSync('/home/jeremy/projets/QueZap/.audit/sim2.log', log.join('\n')); };
const sleep = ms => new Promise(r => setTimeout(r, ms));
const shot = async (page, name, full = false) => { await sleep(900); await page.screenshot({ path: OUT + name + '.png', fullPage: full }).catch(e => L('SHOTFAIL', name, e.message.slice(0, 100))); L('SHOT', name, page.url()); };
const api = async (path, method = 'GET', body, tok, extra = {}) => {
  const r = await fetch(B + path, { method, headers: { 'content-type': 'application/json', ...(tok ? { authorization: 'Bearer ' + tok } : {}), ...extra }, body: body ? JSON.stringify(body) : undefined });
  let j = null; try { j = await r.json(); } catch {}
  return { s: r.status, j };
};
const step = async (name, fn) => { try { await fn(); } catch (e) { L('STEPFAIL', name, e.message.split('\n')[0]); } };
(async () => {
  const lg = await api('/auth/login', 'POST', { identifier: 'admin@quezap.com', password: 'Password123!' });
  const tok = lg.j.accessToken;
  const ql = await api('/quezaps?size=50', 'GET', null, tok);
  const qz = ql.j.data.find(x => x.totalQuestions >= 3);
  const ses = await api('/sessions', 'POST', { name: 'Audit Live ' + Date.now() % 1000, quezapId: qz.id }, tok);
  const { id, code } = ses.j; L('SESSION', id, code);
  const browser = await chromium.launch(); browser.on("disconnected",()=>L("BROWSER CLOSED"));
  const host = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const hp = await host.newPage();
  hp.on('console', m => { if (m.type() === 'error') L('HOSTERR', m.text().slice(0, 200)); });
  await hp.goto(BASE + '/auth/login');
  await hp.locator('input[type="password"]').first().fill('Password123!');
  await hp.locator('input').first().fill('admin@quezap.com');
  await hp.keyboard.press('Enter');
  await hp.waitForURL(/admin/, { timeout: 10000 }).catch(e => L('LOGINFAIL', e.message));
  await hp.goto(BASE + '/admin/sessions/' + id + '/present');
  await sleep(1500);

  // UI players
  const names = ['Léa', 'Maximilien-Alexandre-de-la-Tour-d-Auvergne-Lorraine', 'Zoé 🦊'];
  const players = [];
  for (const [i, n] of names.entries()) {
    const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true, deviceScaleFactor: 2 });
    const p = await ctx.newPage();
    p.on('console', m => { if (m.type() === 'error') L('P' + i + 'ERR', m.text().slice(0, 160)); });
    await p.goto(BASE + '/quizz/join/' + code);
    await step('join' + i, async () => {
      await p.locator('#nickname-value').fill(n);
      if (i === 0) await shot(p, 'player_02_nickname_filled');
      await p.locator('button[type=submit]').click();
      await sleep(1500);
    });
    L('P' + i, 'url', p.url(), 'body:', (await p.locator('body').innerText()).replace(/\n/g, ' | ').slice(0, 200));
    players.push(p);
  }
  await shot(players[0], 'player_03_lobby');
  await shot(players[1], 'player_03b_lobby_long_name');
  // 32 more via API => 35 total
  const tokens = [];
  for (let i = 0; i < 32; i++) {
    const r = await api('/sessions/join', 'POST', { code, participantName: 'Eleve' + (i + 1) });
    if (r.s !== 200 && r.s !== 201) L('APIJOIN', r.s, JSON.stringify(r.j));
    else tokens.push(r.j);
  }
  L('TOKENS sample', JSON.stringify(tokens[0]));
  await shot(hp, 'host_21_lobby_35_players', true);
  await shot(hp, 'host_21b_lobby_35_viewport');
  await shot(players[0], 'player_03c_lobby_35');
  // duplicate name & edge cases
  const dup = await api('/sessions/join', 'POST', { code, participantName: 'léa' }); L('DUPNAME', dup.s, JSON.stringify(dup.j));
  const bad = await api('/sessions/join', 'POST', { code, participantName: '<script>alert(1)</script>' }); L('XSSNAME', bad.s, JSON.stringify(bad.j));
  const huge = await api('/sessions/join', 'POST', { code, participantName: 'x'.repeat(300) }); L('HUGENAME', huge.s, JSON.stringify(huge.j));

  // start
  await step('start', async () => { await hp.getByRole('button', { name: /Démarrer le Quizz/ }).click(); });
  await sleep(1500);
  await shot(hp, 'host_22_question_running');
  await shot(players[0], 'player_04_active_vote_boolean');
  L('P0 body', (await players[0].locator('body').innerText()).replace(/\n/g, ' | ').slice(0, 300));
  L('P0 buttons', JSON.stringify(await players[0].locator('button').allInnerTexts()));
  fs.writeFileSync('/tmp/audit_state.json', JSON.stringify({ id, code }));
  // vote
  await step('vote0', async () => { await players[0].locator('quizz-question-answer').nth(0).click({timeout:4000}); });
  await sleep(500);
  await shot(players[0], 'player_05_feedback_after_vote');
  {const t0=Date.now(); const rs=await Promise.all(tokens.slice(0,30).map(t=>api('/sessions/'+id+'/answers','POST',{answerIndex:Math.random()<0.5?0:1,slideIndex:1},null,{'X-Participant-Token':t.participationToken}))); const c={}; rs.forEach(r=>c[r.s]=(c[r.s]||0)+1); L('CONCURRENT 30 ANSWERS',JSON.stringify(c),(Date.now()-t0)+'ms', JSON.stringify(rs.find(r=>r.s>=400)?.j||''));}
  await shot(hp, 'host_23_question_votes_counting');
  // host reload mid-question
  await hp.reload(); await sleep(2000);
  await shot(hp, 'host_24_question_after_host_reload');
  L('HOST RELOAD body', (await hp.locator('body').innerText()).replace(/\n/g, ' | ').slice(0, 300));
  // player reload
  await players[1].reload(); await sleep(2000);
  await shot(players[1], 'player_06_after_reload_mid_question');
  await step('reveal', async () => { await hp.getByRole('button', { name: /Révéler/ }).click({timeout:4000}); });
  await sleep(1500);
  await shot(hp, 'host_25_reveal');
  {const r=await api('/sessions/'+id+'/answers','POST',{answerIndex:0,slideIndex:1},null,{'X-Participant-Token':tokens[31].participationToken}); L('LATE ANSWER AFTER REVEAL',r.s,JSON.stringify(r.j));}
  await shot(players[0], 'player_07_reveal_result');
  await shot(players[1], 'player_07b_reveal_result_other');
  // next questions
  for (let q = 2; q <= 3; q++) {
    await step('next' + q, async () => { await hp.getByRole('button', { name: /Question suivante|Podium/ }).click({timeout:4000}); });
    await sleep(1500);
    await shot(hp, 'host_2' + (q + 4) + '_question_' + q);
    await shot(players[0], 'player_08_question_' + q);
    await step('vote' + q, async () => { await players[0].locator('quizz-question-answer').nth(0).click({timeout:4000}); });
    await sleep(500);
    await step('reveal' + q, async () => { await hp.getByRole('button', { name: /Révéler/ }).click({timeout:4000}); });
    await sleep(1500);
    await shot(hp, 'host_2' + (q + 6) + '_reveal_' + q);
    await shot(players[0], 'player_09_reveal_' + q);
  }
  await step('podium', async () => { await hp.getByRole('button', { name: /Podium/ }).click({timeout:4000}); });
  await sleep(6000);
  await shot(hp, 'host_30_podium');
  await shot(players[0], 'player_10_end_of_game');
  await shot(players[2], 'player_10b_end_of_game_p3');
  await hp.goto(BASE + '/admin/sessions/' + id + '/report'); await sleep(2000);
  await shot(hp, 'host_31_session_report', true);
  await browser.close();
})().catch(e => { L('FATAL', e.stack); process.exit(1); });

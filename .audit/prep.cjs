const B = 'http://localhost:8000/apiv1';
(async () => {
  const r = await fetch(B + '/auth/login', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ identifier: 'admin@quezap.com', password: 'Password123!' }) });
  const j = await r.json();
  console.log(r.status, Object.keys(j), JSON.stringify(j).slice(0, 200));
  const tok = j.accessToken || j.token;
  const q = await (await fetch(B + '/quezaps?size=50', { headers: { authorization: 'Bearer ' + tok } })).json();
  console.log(JSON.stringify(q).slice(0, 1200));
})();

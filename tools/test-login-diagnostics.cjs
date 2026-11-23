const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const script = fs.readFileSync(path.join(__dirname,
  '../base/host-android/src/main/assets/login-diagnostics.js'), 'utf8');

function setup(response) {
  const messages = [];
  let calls = 0;
  class XHR {
    open() {}
    addEventListener(event, callback) { this.callback = callback; }
  }
  const context = vm.createContext({ URL, location: { href: 'https://accounts.spotify.com/login' },
    XMLHttpRequest: XHR, MyspotiLoginDiagnostics: { postMessage: v => messages.push(JSON.parse(v)) },
    fetch: async () => { calls++; return response; } });
  context.window = context;
  vm.runInContext(script, context);
  return { context, messages, calls: () => calls, XHR };
}

test('observes an error without consuming its body or repeating the login', async () => {
  const body = JSON.stringify({ error: { code: 'INVALID_CAPTCHA', message: 'Captcha failed for person@example.com' },
    email: 'person@example.com', token: 'secret-session', code: '123456' });
  const response = new Response(body, { status: 400 });
  const state = setup(response);
  const result = await state.context.fetch('https://accounts.spotify.com/login/code/send');
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(result, response);
  assert.equal(await result.text(), body);
  assert.equal(state.calls(), 1);
  assert.equal(state.messages.length, 1);
  assert.deepEqual(state.messages[0].codes, ['INVALID_CAPTCHA']);
  assert.ok(state.messages[0].categories.includes('captcha'));
  assert.doesNotMatch(JSON.stringify(state.messages), /person|example|secret|123456/);
});

test('ignores successful verification, unrelated endpoints, and foreign origins', async () => {
  for (const [url, status] of [
    ['https://accounts.spotify.com/login/code/verify', 200],
    ['https://accounts.spotify.com/api/token', 400],
    ['https://evil.example/login/code/send', 400],
  ]) {
    const state = setup(new Response('{"error":"INVALID_CSRF"}', { status }));
    await state.context.fetch(url);
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(state.messages.length, 0);
  }
});

test('handles XHR errors while discarding free text and session data', () => {
  const state = setup();
  const xhr = new state.XHR();
  xhr.open('POST', 'https://accounts.spotify.com/login/code/send');
  xhr.status = 400;
  xhr.responseType = 'json';
  xhr.response = { error: 'INVALID_CSRF', message: 'Cookie missing: secret123', session: 'private' };
  xhr.callback();
  assert.deepEqual(state.messages[0].codes, ['INVALID_CSRF']);
  assert.doesNotMatch(JSON.stringify(state.messages), /secret|private/);
});

test('non-JSON error bodies are never included and installation is idempotent', async () => {
  const state = setup(new Response('<html>person@example.com secret</html>', { status: 403 }));
  const wrapped = state.context.fetch;
  vm.runInContext(script, state.context);
  assert.equal(state.context.fetch, wrapped);
  await state.context.fetch('https://accounts.spotify.com/login/code/send');
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(state.messages[0].format, 'non-json');
  assert.doesNotMatch(JSON.stringify(state.messages), /person|secret/);
});

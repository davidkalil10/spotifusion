// Observe failed login responses. Never inspect request bodies, headers or cookies.
(() => {
  if (window.__myspotiLoginDiagnostics) return;
  window.__myspotiLoginDiagnostics = true;
  const endpoint = value => {
    try {
      const url = new URL(value, location.href);
      return url.origin === 'https://accounts.spotify.com' &&
        /^\/login\/code\/(send|verify)$/.test(url.pathname) ? url.pathname : null;
    } catch (_) { return null; }
  };
  const fields = new Set(['error', 'errors', 'code', 'reason', 'status', 'message',
    'errorCode', 'error_code', 'error_description', 'description']);
  const describe = text => {
    const codes = new Set(), categories = new Set();
    const visit = (value, key, depth) => {
      if (depth > 4 || value == null) return;
      if (Array.isArray(value)) { value.slice(0, 8).forEach(v => visit(v, key, depth + 1)); return; }
      if (typeof value === 'object') {
        Object.keys(value).filter(k => fields.has(k)).slice(0, 12)
          .forEach(k => visit(value[k], k, depth + 1));
        return;
      }
      if (typeof value !== 'string') return;
      // Free text may echo an email address or one-time code. Only report categories.
      for (const [pattern, name] of [
        [/captcha|recaptcha/i, 'captcha'], [/csrf|cross.site.request/i, 'csrf'],
        [/rate.limit|too.many|throttl/i, 'rate-limit'], [/cookie|session/i, 'session'],
        [/unsupported|browser/i, 'browser'], [/email|e-mail/i, 'email'],
        [/invalid.request|bad.request/i, 'invalid-request']
      ]) if (pattern.test(value)) categories.add(name);
      if (['error', 'code', 'reason', 'status', 'errorCode', 'error_code'].includes(key) &&
          /^(?:[A-Z]+_)+[A-Z]+$|^(?:[a-z]+_)+[a-z]+$/.test(value) && value.length <= 64) codes.add(value);
    };
    if (text.length > 16384) return { format: 'large', codes: [], categories: [] };
    try {
      visit(JSON.parse(text), '', 0);
      return { format: 'json', codes: [...codes].slice(0, 8), categories: [...categories] };
    } catch (_) { return { format: 'non-json', codes: [], categories: [] }; }
  };
  const report = (path, status, text) => {
    if (!path || status < 400) return;
    try { MyspotiLoginDiagnostics.postMessage(JSON.stringify({ path, status, ...describe(text) })); }
    catch (_) {} // Diagnostics must never interfere with sign-in.
  };
  const fetch = window.fetch;
  if (fetch) window.fetch = function (...args) {
    return fetch.apply(this, args).then(response => {
      try {
        const path = endpoint(response.url || (typeof args[0] === 'string' ? args[0] : args[0].url));
        if (path && response.status >= 400) response.clone().text()
          .then(text => report(path, response.status, text)).catch(() => {});
      } catch (_) {}
      return response;
    });
  };
  const open = XMLHttpRequest.prototype.open;
  XMLHttpRequest.prototype.open = function (method, url, ...args) {
    const path = endpoint(url);
    // open() aborts an earlier request; register after it so a reused XHR is not misreported.
    const result = open.call(this, method, url, ...args);
    if (path) this.addEventListener('loadend', () => {
      try {
        const text = this.responseType === 'json' ? JSON.stringify(this.response) :
          (!this.responseType || this.responseType === 'text') ? this.responseText : '';
        report(path, this.status, text);
      } catch (_) {}
    }, { once: true });
    return result;
  };
})();

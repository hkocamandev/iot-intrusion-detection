// auth.js — token storage, the authenticated fetch wrapper and the dashboard session guard.
// Loaded by both login.html and index.html; only index.html calls requireSession().
(() => {
  'use strict';

  const STORAGE_KEY = 'iotAuth';

  function read() {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEY)) || null;
    } catch (_) {
      return null; // corrupt storage is treated as "no session"
    }
  }

  function save(auth) {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
    } catch (_) { /* ignore quota/serialization failures */ }
  }

  function clear() {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch (_) { /* ignore */ }
  }

  function token() {
    const stored = read();
    return stored && stored.token ? stored.token : null;
  }

  function toLogin() {
    clear();
    location.replace('/login.html');
  }

  // Resolves to a promise that never settles, so callers awaiting a session simply stop
  // while the browser navigates away.
  function navigatingAway() {
    return new Promise(() => {});
  }

  async function authFetch(url, options) {
    const opts = Object.assign({}, options);
    const current = token();
    opts.headers = Object.assign({}, opts.headers,
      current ? { Authorization: 'Bearer ' + current } : {});
    const res = await fetch(url, opts);
    // A 401 is the single place an expired or revoked token is handled; there is no timer.
    if (res.status === 401) {
      toLogin();
      throw new Error('unauthorized');
    }
    return res;
  }

  let sessionPromise = null;

  // Validates the stored token against the server rather than trusting localStorage, and
  // returns the authoritative username and role.
  function requireSession() {
    if (!sessionPromise) {
      sessionPromise = (async () => {
        if (!token()) {
          toLogin();
          return navigatingAway();
        }
        let res;
        try {
          res = await fetch('/api/auth/me', {
            headers: { Authorization: 'Bearer ' + token() },
          });
        } catch (_) {
          toLogin();
          return navigatingAway();
        }
        if (!res.ok) {
          toLogin();
          return navigatingAway();
        }
        return res.json();
      })();
    }
    return sessionPromise;
  }

  function signOut() {
    toLogin();
  }

  window.Auth = {
    get token() { return token(); },
    save,
    clear,
    toLogin,
    authFetch,
    requireSession,
    signOut,
  };
})();

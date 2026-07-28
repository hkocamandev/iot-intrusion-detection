// chat.js — conversational drawer over /api/chat. Independent of app.js.
(() => {
  'use strict';

  const STORAGE_KEY = 'iotChat';

  const STARTERS = [
    'Last 10 CRITICAL alerts',
    'ML vs RULE breakdown',
    'Most-targeted source',
  ];

  function newSessionId() {
    try {
      if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID();
    } catch (_) { /* non-secure context */ }
    return 'sess-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 10);
  }

  let state = loadState();

  function loadState() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (parsed && parsed.sessionId && Array.isArray(parsed.messages)) return parsed;
      }
    } catch (_) { /* ignore corrupt storage */ }
    return { sessionId: newSessionId(), messages: [] };
  }

  function saveState() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch (_) { /* ignore quota/serialization failures */ }
  }

  // Escape any value before it is interpolated into HTML. Chat content can echo
  // attacker-controlled fields (e.g. sourceId from network traffic), so it must
  // never be inserted into the DOM as raw markup (stored XSS).
  function esc(v) {
    return String(v ?? '').replace(/[&<>"']/g, (c) => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
  }

  // Assistant answers may echo attacker-controlled fields, so render markdown then
  // sanitize with DOMPurify before it ever reaches the DOM.
  function renderMarkdown(text) {
    return DOMPurify.sanitize(marked.parse(String(text ?? '')), { FORBID_TAGS: ['img'] });
  }

  function bubbleHtml(m) {
    if (m.role === 'user') {
      return `<div class="msg user">${esc(m.content)}</div>`;
    }
    if (m.role === 'error') {
      return `<div class="msg error">${esc(m.content)}` +
        `<button class="retry" data-q="${esc(m.retry)}">retry</button></div>`;
    }
    return `<div class="msg bot">${renderMarkdown(m.content)}</div>`;
  }

  function render() {
    const list = document.getElementById('chat-messages');
    list.innerHTML = state.messages.map(bubbleHtml).join('');
    document.getElementById('chat-chips').style.display =
      state.messages.length ? 'none' : 'flex';
    list.scrollTop = list.scrollHeight;
  }

  function setBusy(on) {
    document.getElementById('chat-typing').style.display = on ? 'block' : 'none';
    document.getElementById('chat-input').disabled = on;
    document.getElementById('chat-send').disabled = on;
    document.getElementById('chat-new').disabled = on;
  }

  async function send(question) {
    const q = (question || '').trim();
    if (!q) return;
    const session = state;
    session.messages.push({ role: 'user', content: q });
    saveState();
    render();
    setBusy(true);
    try {
      const res = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question: q, sessionId: session.sessionId }),
      });
      let data;
      try {
        data = await res.json();
      } catch (_) {
        data = {};
      }
      if (data.sessionId) session.sessionId = data.sessionId;
      if (res.ok) {
        session.messages.push({ role: 'bot', content: data.answer });
      } else {
        session.messages.push({ role: 'error', content: data.answer || 'LLM chat unavailable.', retry: q });
      }
    } catch (_) {
      session.messages.push({ role: 'error', content: 'Network error, please retry.', retry: q });
    } finally {
      setBusy(false);
      if (state === session) {
        saveState();
        render();
      }
    }
  }

  function newChat() {
    state = { sessionId: newSessionId(), messages: [] };
    saveState();
    render();
  }

  function sendFromInput() {
    const input = document.getElementById('chat-input');
    send(input.value);
    input.value = '';
  }

  function init() {
    document.getElementById('chat-toggle').addEventListener('click', () =>
      document.getElementById('chat-drawer').classList.toggle('open'));
    document.getElementById('chat-close').addEventListener('click', () =>
      document.getElementById('chat-drawer').classList.remove('open'));
    document.getElementById('chat-new').addEventListener('click', newChat);
    document.getElementById('chat-send').addEventListener('click', sendFromInput);

    const input = document.getElementById('chat-input');
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendFromInput();
      }
    });

    document.getElementById('chat-messages').addEventListener('click', (e) => {
      if (e.target.classList.contains('retry')) send(e.target.dataset.q);
    });

    const chips = document.getElementById('chat-chips');
    chips.innerHTML = STARTERS.map((s) => `<button class="chip">${esc(s)}</button>`).join('');
    chips.addEventListener('click', (e) => {
      if (e.target.classList.contains('chip')) send(e.target.textContent);
    });

    render();
  }

  init();
})();

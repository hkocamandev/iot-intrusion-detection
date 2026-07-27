// chat.js — conversational drawer over /api/chat. Independent of app.js.
const STORAGE_KEY = 'iotChat';

let state = loadState();

function loadState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed && parsed.sessionId && Array.isArray(parsed.messages)) return parsed;
    }
  } catch (_) { /* ignore corrupt storage */ }
  return { sessionId: crypto.randomUUID(), messages: [] };
}

function saveState() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

// Escape any value before it is interpolated into HTML. Chat content can echo
// attacker-controlled fields (e.g. sourceId from network traffic), so it must
// never be inserted into the DOM as raw markup (stored XSS).
function esc(v) {
  return String(v ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[c]));
}

function bubbleHtml(m) {
  if (m.role === 'user') {
    return `<div class="msg user">${esc(m.content)}</div>`;
  }
  if (m.role === 'error') {
    return `<div class="msg error">${esc(m.content)}` +
      `<button class="retry" data-q="${esc(m.retry)}">tekrar dene</button></div>`;
  }
  return `<div class="msg bot">${esc(m.content)}</div>`;
}

function render() {
  const list = document.getElementById('chat-messages');
  list.innerHTML = state.messages.map(bubbleHtml).join('');
  list.scrollTop = list.scrollHeight;
}

function setBusy(on) {
  document.getElementById('chat-typing').style.display = on ? 'block' : 'none';
  document.getElementById('chat-input').disabled = on;
  document.getElementById('chat-send').disabled = on;
}

async function send(question) {
  const q = (question || '').trim();
  if (!q) return;
  state.messages.push({ role: 'user', content: q });
  saveState();
  render();
  setBusy(true);
  try {
    const res = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question: q, sessionId: state.sessionId }),
    });
    const data = await res.json();
    if (data.sessionId) state.sessionId = data.sessionId;
    if (res.ok) {
      state.messages.push({ role: 'bot', content: data.answer });
    } else {
      state.messages.push({ role: 'error', content: data.answer || 'LLM chat kullanılamıyor.', retry: q });
    }
  } catch (_) {
    state.messages.push({ role: 'error', content: 'Ağ hatası, tekrar dene.', retry: q });
  } finally {
    setBusy(false);
    saveState();
    render();
  }
}

function newChat() {
  state = { sessionId: crypto.randomUUID(), messages: [] };
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

  render();
}

init();

let liveCount = 0;
const charts = {};

function makeChart(id, label) {
  return new Chart(document.getElementById(id), {
    type: 'bar',
    data: { labels: [], datasets: [{ label, data: [], backgroundColor: '#4f8cff' }] },
    options: { plugins: { legend: { display: true } }, scales: { y: { beginAtZero: true } } }
  });
}

function refreshChart(chart, map) {
  chart.data.labels = Object.keys(map);
  chart.data.datasets[0].data = Object.values(map);
  chart.update();
}

// Escape any value before it is interpolated into HTML. Alert fields such as
// sourceId originate from network traffic and are attacker-controlled, so they
// must never be inserted into the DOM as raw markup (stored XSS).
function esc(v) {
  return String(v ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[c]));
}

function row(a) {
  const time = a.createdAt ? new Date(a.createdAt).toLocaleTimeString() : '';
  const score = (a.score == null) ? '' : a.score.toFixed(2);
  return `<tr>
    <td>${esc(time)}</td><td>${esc(a.sourceId)}</td><td>${esc(a.attackType)}</td>
    <td class="sev-${esc(a.severity)}">${esc(a.severity)}</td>
    <td class="src-${esc(a.detectionSource)}">${esc(a.detectionSource)}</td>
    <td>${esc(score)}</td><td>${esc(a.ruleName)}</td></tr>`;
}

function prependRow(a) {
  const tbody = document.getElementById('alert-rows');
  tbody.insertAdjacentHTML('afterbegin', row(a));
  while (tbody.rows.length > 100) tbody.deleteRow(tbody.rows.length - 1);
}

async function loadAlerts() {
  const alerts = await (await Auth.authFetch('/api/alerts')).json();
  document.getElementById('alert-rows').innerHTML = alerts.map(row).join('');
}

async function loadStats() {
  const s = await (await Auth.authFetch('/api/stats')).json();
  document.getElementById('c-total').textContent = s.total;
  const high = (s.bySeverity.HIGH ?? 0) + (s.bySeverity.CRITICAL ?? 0);
  document.getElementById('c-high').textContent = high;
  refreshChart(charts.type, s.byAttackType);
  refreshChart(charts.severity, s.bySeverity);
  refreshChart(charts.source, s.byDetectionSource);
}

function connect() {
  const client = new StompJs.Client({
    webSocketFactory: () => new SockJS('/ws'),
    // The browser cannot set headers on the handshake, so the token rides the CONNECT frame.
    connectHeaders: { Authorization: 'Bearer ' + Auth.token },
    reconnectDelay: 3000
  });
  client.onConnect = () => {
    client.subscribe('/topic/alerts', (msg) => {
      const a = JSON.parse(msg.body);
      prependRow(a);
      liveCount += 1;
      document.getElementById('c-live').textContent = liveCount;
    });
    loadAlerts(); // resync on (re)connect
  };
  client.activate();
}

function renderSession(me) {
  const badge = document.getElementById('session-role');
  badge.textContent = me.role;
  badge.hidden = false;
  document.getElementById('session-user').textContent = me.username;
  document.getElementById('sign-out').addEventListener('click', () => Auth.signOut());
  // The server enforces ADMIN on /api/chat; removing the button is presentation only.
  if (me.role !== 'ADMIN') {
    document.getElementById('chat-toggle').remove();
    document.getElementById('chat-drawer').remove();
  }
}

// Nothing loads until the stored token is confirmed with the server; a missing or expired
// session navigates to /login.html instead of resolving.
Auth.requireSession().then((me) => {
  renderSession(me);
  charts.type = makeChart('chart-type', 'By attack type');
  charts.severity = makeChart('chart-severity', 'By severity');
  charts.source = makeChart('chart-source', 'RULE vs ML');
  loadAlerts();
  loadStats();
  setInterval(loadStats, 10000);
  connect();
});

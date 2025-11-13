function renderTable(rows) {
  const container = document.getElementById('tableContainer');
  container.innerHTML = '';
  if (!rows || rows.length === 0) {
    const p = document.createElement('div'); p.className = 'no-rows'; p.textContent = 'No hay filas.'; container.appendChild(p); return;
  }
  // Collect all column names
  const cols = Array.from(rows.reduce((set, r) => { Object.keys(r).forEach(k => set.add(k)); return set; }, new Set()));
  const table = document.createElement('table'); table.className = 'query-result';
  const thead = document.createElement('thead'); const thr = document.createElement('tr');
  cols.forEach(c => { const th = document.createElement('th'); th.textContent = c; thr.appendChild(th); });
  thead.appendChild(thr); table.appendChild(thead);
  const tbody = document.createElement('tbody');
  rows.forEach(r => {
    const tr = document.createElement('tr');
    cols.forEach(c => { const td = document.createElement('td'); const v = r[c]; td.textContent = v === null || v === undefined ? '' : String(v); tr.appendChild(td); });
    tbody.appendChild(tr);
  });
  table.appendChild(tbody); container.appendChild(table);
}

document.getElementById('run').addEventListener('click', async () => {
  const db = document.getElementById('db').value;
  const sql = document.getElementById('sql').value;
  const body = JSON.stringify({ db, sql });
  const msg = document.getElementById('message');
  msg.textContent = 'Executing...';
  try {
    const resp = await fetch('/query', { method: 'POST', headers: { 'Content-Type':'application/json' }, body });
    const text = await resp.text();
    // Try parse JSON
    try {
      const json = JSON.parse(text);
      // normalize to array
      const rows = Array.isArray(json) ? json : (json === null ? [] : [json]);
      renderTable(rows);
      document.getElementById('json').textContent = JSON.stringify(json, null, 2);
      // open details automatically when result is large
      const details = document.getElementById('jsonDetails'); if (details && rows.length > 0) details.open = false;
      msg.textContent = 'OK';
    } catch (e) {
      // Not JSON, show raw text
      document.getElementById('tableContainer').innerHTML = '';
      document.getElementById('json').textContent = text;
      msg.textContent = 'Respuesta no-JSON';
    }
  } catch (e) {
    msg.textContent = 'Error: ' + e;
    document.getElementById('tableContainer').innerHTML = '';
    document.getElementById('json').textContent = '';
  }
});

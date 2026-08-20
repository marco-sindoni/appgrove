/*
 * Prototipo della console di piattaforma appgrove — listino dei posti (UC 0105).
 *
 * Mostra la cosa che lo sviluppatore ha chiesto: l'amministratore di appgrove cambia le tariffe delle
 * fasce per TUTTI gli account, con effetto dal ciclo di fatturazione successivo.
 *
 * Il punto di sostanza, e la ragione per cui la schermata è fatta così: non si MODIFICA una tariffa,
 * si crea una NUOVA VERSIONE del listino con la sua data di decorrenza. Le versioni già decorse sono
 * immutabili — è il solo modo per poter rispondere, fra un anno, alla domanda «quanto pagava questo
 * cliente in marzo?».
 *
 * Dati inventati, nessuna chiamata di rete.
 */

const VIGENTE = {
  decorrenza: '1 gen 2026', autore: 'appgrove', nota: 'Listino iniziale al lancio.',
  fasce: FASCE.map((f) => ({ ...f })),
};

const STORICO = [
  { decorrenza: '1 gen 2026', autore: 'appgrove', nota: 'Listino iniziale al lancio.', stato: 'vigente' },
];

const stato = {
  schermata: 'listino',
  bozza: VIGENTE.fasce.map((f) => ({ ...f })),
  decorrenza: '2026-10-01',
  nota: '',
  anteprimaAperta: false,
  programmata: null,
};

const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const num = (s) => Number(String(s).replace(',', '.'));

/*
 * Il calcolo del dovuto: la regola di UC 0102 — **a scaglioni progressivi**. Ogni posto paga la tariffa
 * della fascia in cui *quel posto* cade, non la tariffa dell'ultima fascia raggiunta.
 *
 * Esempio con 52 posti: i primi 3 gratis, dal 4° al 10° a 2,99 (sette posti), dall'11° al 50° a 1,99
 * (quaranta posti), il 51° e il 52° a 0,99 (due posti) → 20,93 + 79,60 + 1,98 = 102,51 €.
 *
 * Conseguenza che vale la pena conoscere: il totale è **sempre crescente** col numero dei posti. Il
 * modello precedente (tariffa unica su tutti i posti) faceva scendere il totale ai confini di fascia —
 * aritmeticamente corretto ma impossibile da spiegare a un cliente.
 */
function dovuto(posti, fasce) {
  if (posti <= 0) return 0;
  let tot = 0;
  for (const f of fasce) {
    if (posti < f.da) break;
    const fine = f.a === null ? posti : Math.min(posti, f.a);
    tot += (fine - f.da + 1) * num(f.tariffa);
  }
  return Math.round(tot * 100) / 100;
}

const eur = (v) => v === null ? '—' : v.toFixed(2).replace('.', ',') + ' €';

/* Validazione delle fasce (UC 0105 passo 2): contigue, senza buchi né sovrapposizioni. */
function valida(fasce) {
  const errori = [];
  if (fasce[0].da !== 1) errori.push('La prima fascia deve partire dal posto 1.');
  if (fasce[fasce.length - 1].a !== null) errori.push('L\'ultima fascia non può avere un limite superiore.');
  for (let i = 0; i < fasce.length; i++) {
    if (num(fasce[i].tariffa) < 0) errori.push(`La fascia ${i + 1} ha una tariffa negativa.`);
    if (i > 0) {
      const prec = fasce[i - 1];
      if (prec.a === null) { errori.push(`La fascia ${i} è aperta ma non è l'ultima.`); continue; }
      if (fasce[i].da !== prec.a + 1) {
        errori.push(fasce[i].da > prec.a + 1
          ? `Buco fra il posto ${prec.a} e il posto ${fasce[i].da}: nessuna tariffa lo copre.`
          : `Sovrapposizione: la fascia ${i + 1} inizia a ${fasce[i].da} ma la ${i} arriva a ${prec.a}.`);
      }
    }
  }
  return errori;
}

const CASI_TIPICI = [3, 4, 10, 11, 52, 120];   /* i confini contano più dei valori tondi */

/* Portafoglio inventato, per l'anteprima dell'effetto sul reale. */
const PORTAFOGLIO = [
  { account: 'Studio Marchetti', posti: 5 },
  { account: 'Bianchi Serramenti', posti: 3 },
  { account: 'DS Consulting', posti: 12 },
  { account: 'Greco Trasporti', posti: 9 },
  { account: 'Rossi & Figli', posti: 62 },
  { account: 'Neri Impianti', posti: 2 },
];

function renderSidebar() {
  /* Voci e icone della console vera (frontend/apps/admin/src/shell/Sidebar.tsx), coi suoi raggruppamenti. */
  const voci = [
    { label: 'Panoramica', icona: 'space_dashboard' },
    { label: 'Account', icona: 'corporate_fare' },
    { label: 'Utenti', icona: 'group' },
    { label: 'Diritti', icona: 'apps' },
    { label: 'Applicazioni', icona: 'widgets' },
    { label: 'Fatturazione', icona: 'account_balance_wallet' },
    { label: 'Riconciliazione', icona: 'account_balance' },
    { label: 'Diritti GDPR', icona: 'verified_user' },
    { label: 'Ticket', icona: 'support_agent' },
  ];
  const ico = (n) => `<span class="material-symbols-rounded" aria-hidden="true">${n}</span>`;
  return `<aside class="sidebar">
    <div class="brand">
      <span class="brand-mark">a</span>
      <span class="kv"><span class="brand-name">appgrove</span><span class="brand-sub">Console</span></span>
    </div>
    <nav class="nav" aria-label="Navigazione della console">
      <p class="nav-label">Piattaforma</p>
      ${voci.map((v) => `<button class="nav-item">${ico(v.icona)}${v.label}</button>`).join('')}
      <p class="nav-label" style="padding-top:16px">Prezzi</p>
      <button class="nav-item" aria-current="page">${ico('sell')}Posti — listino</button>
      <button class="nav-item">${ico('price_change')}Listini delle applicazioni</button>
    </nav>
    <div class="sidebar-foot"><div class="user-card">
      <span class="avatar">MS</span>
      <span class="kv"><strong style="font-size:13px">Marco Sindoni</strong>
      <span class="faint" style="font-size:11px">Amministratore di piattaforma</span></span>
    </div></div>
  </aside>`;
}

function tabellaFasce(fasce, { modificabile }) {
  const righe = fasce.map((f, i) => {
    const intervallo = f.a === null ? `da ${f.da} in su` : `${f.da} – ${f.a}`;
    const tariffa = modificabile
      ? `<input class="ag mono" style="width:88px" value="${esc(f.tariffa)}" data-fascia="${i}" aria-label="Tariffa della fascia ${intervallo}" /> €`
      : `<span class="mono">${esc(f.tariffa)} €</span>`;
    return `<tr>
      <td class="mono">${intervallo}</td>
      <td>${tariffa}</td>
      <td class="faint">${esc(f.nota || '')}</td>
    </tr>`;
  }).join('');
  return `<table class="ag">
    <thead><tr><th>Posti</th><th>Tariffa mensile per posto</th><th>Nota</th></tr></thead>
    <tbody>${righe}</tbody></table>`;
}

function renderAnteprima() {
  const errori = valida(stato.bozza);
  if (errori.length) {
    return `<div class="notice notice-warning"><div><strong>Non si può salvare.</strong>
      <ul>${errori.map((e) => `<li>${esc(e)}</li>`).join('')}</ul></div></div>`;
  }
  const casi = CASI_TIPICI.map((p) => {
    const a = dovuto(p, VIGENTE.fasce), b = dovuto(p, stato.bozza);
    const delta = b - a;
    const segno = delta === 0 ? '<span class="badge badge-neutral">invariato</span>'
      : delta > 0 ? `<span class="badge badge-danger">+ ${eur(delta)}</span>`
        : `<span class="badge badge-success">− ${eur(-delta)}</span>`;
    return `<tr><td class="mono">${p} posti</td><td class="mono">${eur(a)}</td><td class="mono">${eur(b)}</td><td>${segno}</td></tr>`;
  }).join('');

  let primaTot = 0, dopoTot = 0, cambiano = 0, rincaroMax = 0, rincariElenco = [];
  PORTAFOGLIO.forEach((c) => {
    const a = dovuto(c.posti, VIGENTE.fasce), b = dovuto(c.posti, stato.bozza);
    primaTot += a; dopoTot += b;
    if (a !== b) cambiano++;
    if (b > a) { rincaroMax = Math.max(rincaroMax, b - a); rincariElenco.push({ ...c, delta: b - a }); }
  });

  return `
    <div class="card"><div class="card-head"><h2>Anteprima — casi tipici</h2></div>
      <div class="card-body">${`<table class="ag"><thead><tr><th>Posti</th><th>Oggi</th><th>Con la nuova versione</th><th>Effetto</th></tr></thead><tbody>${casi}</tbody></table>`}
      <p class="why" style="margin-top:10px">Il listino è a <strong>scaglioni progressivi</strong>: ogni posto paga la
      tariffa della fascia in cui cade — i primi 3 gratis, poi i sette successivi alla prima tariffa, e così via. Il
      totale <strong>cresce sempre</strong>, e il costo del posto successivo <strong>scende</strong> passando di
      fascia: è quello che un cliente si aspetta da uno sconto sul volume.</p>
      </div></div>

    <div class="card"><div class="card-head"><h2>Anteprima — portafoglio reale</h2></div>
      <div class="card-body">
        <div class="grid grid-3" style="margin-bottom:16px">
          <div class="kv"><span class="kv-value mono">${cambiano}</span><span class="kv-label">account che cambiano importo</span></div>
          <div class="kv"><span class="kv-value mono">${eur(primaTot)}</span><span class="kv-label">incasso mensile oggi</span></div>
          <div class="kv"><span class="kv-value mono">${eur(dopoTot)}</span><span class="kv-label">incasso mensile dopo</span></div>
          <div class="kv"><span class="kv-value mono">${eur(rincaroMax)}</span><span class="kv-label">rincaro massimo su un account</span></div>
        </div>
        ${rincariElenco.length ? `<div class="notice notice-warning"><div><strong>${rincariElenco.length} account subiscono un rincaro.</strong>
          Vanno avvisati prima della decorrenza: l'elenco è disponibile per la comunicazione.
          <table class="ag" style="margin-top:8px">${rincariElenco.map((c) => `<tr><td>${esc(c.account)}</td><td class="mono">${c.posti} posti</td><td class="mono">+ ${eur(c.delta)}</td></tr>`).join('')}</table>
          </div></div>` : '<p class="muted" style="margin:0;font-size:13px">Nessun account subisce un rincaro con questa versione.</p>'}
        <p class="why" style="margin-top:10px">Nomi di <strong>account</strong>, non di persone: l'anteprima non tratta dati personali.</p>
      </div></div>`;
}

function contenuto() {
  const programmata = stato.programmata ? `
    <div class="notice notice-accent"><div><strong>Versione programmata.</strong>
      Decorre dal <strong>${esc(stato.programmata.decorrenza)}</strong> e non è ancora attiva: fino a quel giorno
      tutti gli account continuano a pagare col listino vigente.
      <div class="row" style="margin-top:9px">
        <button class="btn btn-secondary btn-sm" data-azione="annulla-programmata">Annulla la versione programmata</button>
      </div></div></div>` : '';

  return `
    <div><h1 class="page-title">Posti — listino</h1>
      <p class="page-sub">Le tariffe delle fasce dei posti, valide per <strong>tutti gli account</strong>. Un cambio non si applica mai al periodo in corso.</p></div>

    ${programmata}

    <div class="card"><div class="card-head"><h2>Versione vigente</h2>
      <span class="badge badge-success">dal ${esc(VIGENTE.decorrenza)}</span></div>
      <div class="card-body">${tabellaFasce(VIGENTE.fasce, { modificabile: false })}
      <p class="why" style="margin-top:10px">Nota: ${esc(VIGENTE.nota)} — le versioni già decorse sono <strong>immutabili</strong>.</p></div></div>

    <div class="card"><div class="card-head"><h2>Nuova versione</h2></div>
      <div class="card-body">
        ${tabellaFasce(stato.bozza, { modificabile: true })}
        <div class="row" style="margin-top:16px;align-items:flex-end">
          <label class="kv"><span class="kv-label">Decorrenza</span>
            <input class="ag" type="date" value="${esc(stato.decorrenza)}" data-campo="decorrenza" />
            <span class="why">Non prima di 30 giorni: serve il tempo di avvisare i clienti.</span></label>
          <label class="kv" style="flex:1;min-width:260px"><span class="kv-label">Perché questo cambio (obbligatorio)</span>
            <textarea class="ag" rows="2" data-campo="nota" placeholder="Fra sei mesi nessuno ricorderà il motivo.">${esc(stato.nota)}</textarea></label>
        </div>
        <div class="row" style="margin-top:16px">
          <button class="btn btn-secondary" data-azione="anteprima">${stato.anteprimaAperta ? 'Aggiorna l\'anteprima' : 'Calcola l\'anteprima dell\'effetto'}</button>
          ${stato.nota.trim() && stato.anteprimaAperta && !valida(stato.bozza).length
    ? '<button class="btn btn-primary" data-azione="salva">Crea la versione</button>'
    : '<span class="kv"><button class="btn btn-primary" aria-disabled="true" aria-describedby="why-salva">Crea la versione</button><span class="why" id="why-salva">Servono l\'anteprima calcolata, fasce valide e la nota del motivo.</span></span>'}
        </div>
      </div></div>

    ${stato.anteprimaAperta ? renderAnteprima() : ''}

    <div class="card"><div class="card-head"><h2>Storico delle versioni</h2></div>
      <div class="card-body"><table class="ag">
        <thead><tr><th>Decorrenza</th><th>Autore</th><th>Nota</th><th>Stato</th></tr></thead>
        <tbody>${[...(stato.programmata ? [{ ...stato.programmata, stato: 'programmata' }] : []), ...STORICO].map((v) => `
          <tr><td class="mono">${esc(v.decorrenza)}</td><td>${esc(v.autore)}</td><td class="muted">${esc(v.nota)}</td>
          <td>${v.stato === 'vigente' ? '<span class="badge badge-success">vigente</span>' : '<span class="badge badge-accent">programmata</span>'}</td></tr>`).join('')}
        </tbody></table>
        <p class="why" style="margin-top:10px">Una versione passata si può <strong>vedere</strong>, mai modificare. Una correzione è una versione nuova.</p>
      </div></div>`;
}

function render() {
  document.body.innerHTML = `${renderSidebar()}
    <div class="main">
      <div class="x-proto-scaffold-bar"><strong>Prototipo · epica 22</strong>
      <span class="x-proto-scaffold-tag" title="Questa barra è impalcatura del prototipo: non fa parte del prodotto e non va implementata.">non è prodotto</span>
        <span style="opacity:.6">console di piattaforma appgrove</span>
        <a href="owner.html" style="margin-left:auto">← Torna al backoffice del cliente</a></div>
      <div class="topbar">Console appgrove · amministratore di piattaforma</div>
      <div class="content">${contenuto()}</div>
    </div>`;
}

document.addEventListener('input', (e) => {
  const f = e.target.closest('[data-fascia]');
  if (f) { stato.bozza[Number(f.dataset.fascia)].tariffa = f.value; return; }
  const c = e.target.closest('[data-campo]');
  if (c) { stato[c.dataset.campo] = c.value; }
});

document.addEventListener('click', (e) => {
  const az = e.target.closest('[data-azione]');
  if (!az) return;
  const a = az.dataset.azione;
  if (a === 'anteprima') stato.anteprimaAperta = true;
  if (a === 'salva') {
    stato.programmata = { decorrenza: stato.decorrenza, autore: 'appgrove', nota: stato.nota };
    stato.anteprimaAperta = false;
    alert('Prototipo: la versione è creata e PROGRAMMATA. Nessun account cambia importo prima della decorrenza, e i periodi già fatturati restano immutabili (UC 0105).');
  }
  if (a === 'annulla-programmata') stato.programmata = null;
  render();
});

render();

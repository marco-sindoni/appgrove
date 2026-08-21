/*
 * Prototipi dell'epica 22 — costruzione dell'interfaccia.
 *
 * Un solo file rende tutti e quattro i prototipi per ruolo: la pagina dichiara il proprio ruolo con
 * `<body data-role="owner">` e da lì tutto discende dalla MATRICE (proto-data.js). È voluto: se la
 * differenza fra i ruoli vivesse in quattro copie di HTML, i quattro file divergerebbero e il
 * prototipo mentirebbe.
 *
 * Niente librerie, niente chiamate di rete. Si apre con un doppio clic.
 */

const RUOLO = document.body.dataset.role;
const R = MATRICE[RUOLO];

/* Stato della navigazione, deliberatamente semplice: schermata corrente + finti stati d'esempio. */
const stato = {
  schermata: location.hash.replace('#', '') || 'dashboard',
  riduzioneInAttesa: false,           // interruttore didattico, solo nel prototipo owner
  richiesteInviate: {},               // «chiedi all'owner di installare»
  pannelloAperto: false,          // si apre col pulsante in basso a destra: non deve coprire il contenuto
  selettoreAperto: false,         // selettore dell'account attivo (UC 0117)
  invitoAccountChiuso: false,     // «Non ora» sull'invito ad aprire un proprio account (UC 0108): nel prodotto
                                  // vale una settimana, e l'invito stesso vive un anno dall'iscrizione
};

const MIE_APPARTENENZE = APPARTENENZE[RUOLO] || [];
const MIEI_INVITI = INVITI_RICEVUTI[RUOLO] || [];
const APPARTENENZA_ATTIVA = MIE_APPARTENENZE.find((a) => a.attiva) || { nome: ACCOUNT.nome };

/* Chi collabora SOLO negli account di altri: nessuna sua appartenenza porta il ruolo `owner`.
 * Non è una condizione di ruolo — un `admin` può benissimo avere un proprio account (è il caso di
 * admin.html, owner di «Rinaldi Design») — ma una condizione sull'INSIEME delle appartenenze. Da qui
 * discende l'invito ad aprirne uno proprio nel cruscotto (UC 0108 §4.5). */
const SENZA_ACCOUNT_PROPRIO = MIE_APPARTENENZE.length > 0
  && !MIE_APPARTENENZE.some((a) => a.ruolo === 'owner');

const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));

/*
 * Rende un comando rispettando la matrice: utilizzabile, disabilitato con spiegazione, o assente.
 *
 * `spiegazione: 'visibile'` scrive il motivo sotto il comando; `'nascosta'` lo tiene solo negli
 * attributi leggibili dagli strumenti di assistenza. La distinzione serve a non ripetere la stessa
 * frase su ogni riga di una tabella: là il motivo è identico dodici volte e diventa rumore, mentre
 * sul comando principale della pagina va detto per esteso.
 */
function comando({ permesso, etichetta, variante = 'primary', piccolo = false, motivo, onClick, spiegazione = 'visibile' }) {
  if (permesso === 'no') return '';
  const cls = `btn btn-${variante}${piccolo ? ' btn-sm' : ''}`;
  if (permesso === 'sola-lettura') {
    const testo = motivo || 'Non hai il ruolo necessario per questa operazione.';
    if (spiegazione === 'nascosta') {
      return `<button class="${cls}" aria-disabled="true" title="${esc(testo)}" aria-label="${esc(etichetta)} — ${esc(testo)}">${esc(etichetta)}</button>`;
    }
    const id = 'why-' + Math.random().toString(36).slice(2, 8);
    return `<span class="kv" style="align-items:flex-start"><button class="${cls}" aria-disabled="true" aria-describedby="${id}">${esc(etichetta)}</button>
      <span class="why" id="${id}">${esc(testo)}</span></span>`;
  }
  return `<button class="${cls}" ${onClick ? `data-azione="${onClick}"` : ''}>${esc(etichetta)}</button>`;
}

/* ── Menu laterale ────────────────────────────────────────────────────────── */

/* I nomi delle icone sono quelli VERI di shell/Sidebar.tsx: si copiano, non si scelgono. */
const VOCI_PIATTAFORMA = [
  { id: 'dashboard', chiave: 'dashboard', label: 'Dashboard', icona: 'space_dashboard' },
  { id: 'catalog', chiave: 'catalog', label: 'App catalog', icona: 'apps' },
  { id: 'account', chiave: 'account', label: 'Account', icona: 'account_circle' },
  { id: 'billing', chiave: 'billing', label: 'Billing', icona: 'credit_card' },
  { id: 'members', chiave: 'members', label: 'Members', icona: 'group' },
  { id: 'privacy', chiave: 'privacy', label: 'I miei dati', icona: 'shield_person' },
  { id: 'support', chiave: 'support', label: 'Supporto', icona: 'support_agent' },
];

/* Icona come nel prodotto (design-system/Icon.tsx): stesso font, stesso nome di classe. */
const ico = (nome) => `<span class="material-symbols-rounded" aria-hidden="true">${nome}</span>`;

function renderSidebar() {
  const voci = VOCI_PIATTAFORMA
    .filter((v) => R.nav[v.chiave] !== 'no')
    .map((v) => {
      /* Contatore sulla voce del cruscotto: da un'altra schermata l'invito resterebbe invisibile, e un
         invito non risposto è un rapporto di lavoro in sospeso, non un dettaglio. */
      const conta = v.id === 'dashboard' && MIEI_INVITI.length ? ` <span class="badge badge-accent">${MIEI_INVITI.length}</span>` : '';
      return `<button class="nav-item" data-vai="${v.id}" ${stato.schermata === v.id ? 'aria-current="page"' : ''}>${ico(v.icona)}${v.label}${conta}</button>`;
    })
    .join('');

  /* Intersezione a TRE (UC 0107): registro dei moduli ∩ diritti dell'account ∩ accessi della persona. */
  const app = APPLICAZIONI.filter((a) => R.appVisibili.includes(a.id)).map((a) => {
    const sezioni = a.sezioni.map((s) => {
      const id = `${a.id}-${s.id}`;
      return `<button class="nav-item" data-vai="${id}" ${stato.schermata === id ? 'aria-current="page"' : ''}>${ico(s.icona)}${s.label}</button>`;
    }).join('');
    return `<div>
      <div class="nav-item" style="color:rgb(var(--ag-text))">
        <span class="avatar" style="width:24px;height:24px;border-radius:7px;background:rgb(var(--ag-${a.tinta}) / .16);color:rgb(var(--ag-${a.tinta}))"><span class="material-symbols-rounded" style="font-size:15px;font-variation-settings:'FILL' 1" aria-hidden="true">${a.icona}</span></span>
        ${a.nome}
      </div>
      <div class="nav-sub">${sezioni}</div>
    </div>`;
  }).join('');

  const vuoto = `<p class="muted" style="padding:8px 10px;font-size:13px">Nessuna applicazione ti è stata abilitata.</p>`;
  const me = PERSONE.find((p) => (RUOLO === 'owner' ? p.owner : p.nome.startsWith(RUOLO === 'admin' ? 'Marta' : RUOLO === 'editor' ? 'Luca' : 'Sara')));

  return `<aside class="sidebar">
    <div class="brand">
      <span class="brand-mark">a</span>
      <span class="kv"><span class="brand-name">appgrove</span><span class="brand-sub">Workspace</span></span>
    </div>
    <!-- Selettore dell'account SOTTO il marchio (UC 0117): l'account è il contesto in cui si sta
         lavorando, quindi sta col marchio e col menu, non fra i controlli dell'intestazione. -->
    <div class="account-slot">${renderSelettore()}</div>
    <nav class="nav" aria-label="Navigazione">
      <p class="nav-label">Piattaforma</p>
      ${voci}
      <p class="nav-label" style="padding-top:16px">Le tue applicazioni</p>
      ${app || vuoto}
    </nav>
    <div class="sidebar-foot">
      <!-- Piede del menu, come nel prodotto (shell/Sidebar.tsx): «Impostazioni» + scheda della persona
           con il suo menu (Sicurezza, Esci). Sono preferenze e atti PERSONALI: visibili a ogni ruolo. -->
      <button class="nav-item" data-vai="settings" ${stato.schermata === 'settings' ? 'aria-current="page"' : ''}>${ico('settings')}Impostazioni</button>
      <!-- Come nel prodotto: elemento details nativo, nessuno stato da gestire (shell/Sidebar.tsx). -->
      <details class="user-details">
        <summary class="user-card">
          <span class="avatar">${me ? esc(me.nome.split(' ').map((x) => x[0]).join('')) : '—'}</span>
          <span class="kv" style="min-width:0">
            <strong style="font-size:13px">${me ? esc(me.nome) : ''}</strong>
            <!-- Nessun ruolo mostrato ai collaboratori: non ne hanno uno globale (UC 0107). E nessun nome
                 di account: ora sta nel selettore, sopra — ripeterlo qui sarebbe rumore. -->
            <span class="faint" style="font-size:11px">${R.platformRole === 'owner' ? 'Titolare dell\'account' : esc(me ? me.email : '')}</span>
          </span>
          <span class="material-symbols-rounded" style="margin-left:auto;opacity:.5;font-size:18px" aria-hidden="true">expand_more</span>
        </summary>
        <div class="user-menu">
          <button class="nav-item" data-vai="security" ${stato.schermata === 'security' ? 'aria-current="page"' : ''}>${ico('shield')}Sicurezza</button>
          <button class="nav-item" data-azione="esci">${ico('logout')}Esci</button>
        </div>
      </details>
    </div>
  </aside>`;
}

/* ── Schermate ────────────────────────────────────────────────────────────── */

function schermataDashboard() {
  const schede = APPLICAZIONI.filter((a) => R.appVisibili.includes(a.id)).map((a) => `
    <div class="card"><div class="card-body">
      <div class="row" style="margin-bottom:10px">
        <span class="avatar" style="border-radius:10px;background:rgb(var(--ag-${a.tinta}) / .16);color:rgb(var(--ag-${a.tinta}))"><span class="material-symbols-rounded" style="font-variation-settings:'FILL' 1" aria-hidden="true">${a.icona}</span></span>
        <strong style="font-size:15px">${a.nome}</strong>
      </div>
      <p class="muted" style="margin:0 0 12px;font-size:13px">Il tuo ruolo: <strong>${RUOLO_UMANO[R.appRoles[a.id]] || '—'}</strong></p>
      <div class="row">
        <button class="btn btn-primary btn-sm" data-vai="${a.id}-${a.sezioni[0].id}">Apri</button>
        ${comando({ permesso: R.permessi.gestisciPiano, etichetta: 'Gestisci il piano', variante: 'ghost', piccolo: true })}
      </div>
    </div></div>`).join('');

  const bloccoEconomico = R.platformRole === 'owner' ? `
    <div class="grid grid-3">
      <div class="card"><div class="card-body kv"><span class="kv-value mono">${POSTI.spesaTotale}</span><span class="kv-label">Spesa mensile</span></div></div>
      <div class="card"><div class="card-body kv"><span class="kv-value mono">2</span><span class="kv-label">Applicazioni attive</span></div></div>
      <div class="card"><div class="card-body kv"><span class="kv-value mono">5</span><span class="kv-label">Posti usati</span></div></div>
    </div>` : '';

  return `
    <div><h1 class="page-title">Buongiorno</h1>
      <p class="page-sub">${R.platformRole === 'owner' ? esc(ACCOUNT.nome) : 'Le applicazioni a cui sei abilitato.'}</p></div>
    ${sezioneInviti()}
    ${sezioneAccountProprio()}
    ${bloccoEconomico}
    <div class="grid grid-3">${schede || '<div class="card"><div class="card-body"><p class="muted" style="margin:0">Il titolare dell\'account non ti ha ancora abilitato a nessuna applicazione. Puoi guardare il <a href="#catalog">catalogo</a> e chiedergli di installare quello che ti serve, oppure scrivere al <a href="#support">supporto</a>.</p></div></div>'}</div>`;
}

function schermataMembers() {
  const righe = PERSONE.map((p) => {
    const nApp = p.owner ? APPLICAZIONI.length : Object.keys(p.accessi).length;
    const stat = p.stato === 'invito'
      ? '<span class="badge badge-warning">Invito in attesa</span>'
      : (stato.riduzioneInAttesa && (p.id === 'u4'))
        ? '<span class="badge badge-danger">In cessazione dal 14 set</span>'
        : '<span class="badge badge-success">Attiva</span>';
    return `<tr>
      <td><strong>${esc(p.nome)}</strong>${p.owner ? ' <span class="badge badge-accent">Titolare</span>' : ''}</td>
      <td class="muted">${esc(p.email)}</td>
      <td>${stat}</td>
      <td>${nApp === 0 ? '<span class="faint">nessuna applicazione</span>' : `<a href="#crm-utenti">${nApp} applicazion${nApp === 1 ? 'e' : 'i'}</a>`}</td>
      <td class="muted">${esc(p.dal)}</td>
      <td>${p.owner ? '<span class="faint">—</span>' : '<button class="btn btn-secondary btn-sm">Sospendi</button>'}</td>
    </tr>`;
  }).join('');

  const avviso = stato.riduzioneInAttesa ? `
    <div class="notice notice-warning">
      <div><strong>Riduzione programmata.</strong> 1 persona cesserà il <strong>14 settembre</strong>.
      Fino ad allora non puoi aggiungere persone: il posto è pagato e resta operativo.
      <div class="row" style="margin-top:9px">
        <button class="btn btn-secondary btn-sm" data-azione="annulla-riduzione">Annulla la riduzione</button>
      </div></div>
    </div>` : '';

  const invito = stato.riduzioneInAttesa
    ? comando({ permesso: 'sola-lettura', etichetta: 'Invita una persona', motivo: 'C\'è una riduzione di posti in attesa: annullala, oppure attendi il 14 settembre.' })
    : comando({ permesso: R.permessi.invitaPersone, etichetta: 'Invita una persona', onClick: 'invita' });

  return `
    <div><h1 class="page-title">Members</h1>
      <p class="page-sub">Le persone del tuo gruppo di lavoro. <strong>I permessi si assegnano dentro ogni applicazione</strong>, non qui.</p></div>

    ${avviso}

    <!-- Riquadro dei posti (UC 0103): l'effetto economico è in testa, non nascosto in fatturazione. -->
    <div class="card"><div class="card-body">
      <div class="spread">
        <div class="kv">
          <span class="kv-value mono">${POSTI.usati} posti usati</span>
          <span class="kv-label">${POSTI.attive} attive · ${POSTI.inviti} invito in attesa${stato.riduzioneInAttesa ? ' · 1 in cessazione' : ''}</span>
        </div>
        <div class="kv">
          <span class="kv-value mono">${POSTI.dovuto} / mese</span>
          <span class="kv-label">${POSTI.calcolo} — a scaglioni</span>
        </div>
        <div class="kv">
          <span class="kv-value mono">+ ${POSTI.prossimo}</span>
          <span class="kv-label">il prossimo posto → totale ${POSTI.dovutoDopo}</span>
        </div>
        <div class="row">${invito}
          ${comando({ permesso: stato.riduzioneInAttesa ? 'no' : R.permessi.riduciPosti, etichetta: 'Riduci i posti', variante: 'secondary', onClick: 'riduci' })}
        </div>
      </div>
    </div></div>

    <div class="card">
      <div class="card-head"><h2>Persone</h2></div>
      <div class="card-body">
        <table class="ag">
          <thead><tr><th>Nome</th><th>Email</th><th>Stato</th><th>Applicazioni</th><th>Dal</th><th>Azioni</th></tr></thead>
          <tbody>${righe}</tbody>
        </table>
        <p class="why" style="margin-top:12px">Nessuna colonna «ruolo»: il ruolo non è della persona, è del suo accesso a una singola applicazione (epica 22).</p>
      </div>
    </div>`;
}

function schermataContatti(appId) {
  const app = APPLICAZIONI.find((a) => a.id === appId);
  const ruolo = R.appRoles[appId];
  const righe = CONTATTI.map((c) => `<tr>
    <td><strong>${esc(c.nome)}</strong></td>
    <td class="muted">${esc(c.azienda)}</td>
    <td><span class="badge badge-neutral">${esc(c.fase)}</span></td>
    <td class="mono">${esc(c.valore)}</td>
    <td><div class="row">
      ${comando({ permesso: R.permessi.modificaContatto, etichetta: 'Modifica', variante: 'secondary', piccolo: true, spiegazione: 'nascosta', motivo: 'Serve il ruolo Editor su questa applicazione.' })}
      ${comando({ permesso: R.permessi.eliminaContatto, etichetta: 'Elimina', variante: 'danger', piccolo: true, spiegazione: 'nascosta', motivo: 'Serve il ruolo Editor su questa applicazione.' })}
    </div></td>
  </tr>`).join('');

  return `
    <div class="spread">
      <div><h1 class="page-title">${app.nome} — Contatti</h1>
        <p class="page-sub">Il tuo ruolo su questa applicazione: <strong>${esc(ruolo)}</strong> (${RUOLO_UMANO[ruolo]}).</p></div>
      ${comando({ permesso: R.permessi.creaContatto, etichetta: 'Nuovo contatto', motivo: 'Serve il ruolo Editor su questa applicazione: chiedilo al titolare dell\'account o a un amministratore dell\'applicazione.' })}
    </div>
    <div class="card"><div class="card-body">
      <table class="ag">
        <thead><tr><th>Contatto</th><th>Azienda</th><th>Fase</th><th>Valore</th><th>Azioni</th></tr></thead>
        <tbody>${righe}</tbody>
      </table>
    </div></div>
    <div class="notice notice-info">
      <div><strong>Come si leggono i ruoli in questa applicazione.</strong>
      <ul><li><strong>viewer</strong> — consulta contatti e trattative, non le modifica.</li>
      <li><strong>editor</strong> — crea, modifica ed elimina contatti e trattative.</li>
      <li><strong>admin</strong> — come editor, e in più abilita altre persone dell'account a questa applicazione.</li></ul></div>
    </div>`;
}

function schermataUtentiApp(appId) {
  const app = APPLICAZIONI.find((a) => a.id === appId);
  const puoGestire = R.permessi.gestisciUtentiApp === 'si';
  const abilitati = PERSONE.filter((p) => p.owner || p.accessi[appId]);

  const righe = abilitati.map((p) => {
    const ruolo = p.owner ? 'owner' : p.accessi[appId];
    const cella = p.owner
      ? '<span class="badge badge-accent">Titolare dell\'account</span>'
      : puoGestire
        ? `<select class="ag" aria-label="Ruolo di ${esc(p.nome)} su ${esc(app.nome)}">
             <option ${ruolo === 'viewer' ? 'selected' : ''}>viewer</option>
             <option ${ruolo === 'editor' ? 'selected' : ''}>editor</option>
             <option ${ruolo === 'admin' ? 'selected' : ''}>admin</option>
           </select>`
        : `<span>${esc(ruolo)}</span>`;
    return `<tr>
      <td><strong>${esc(p.nome)}</strong></td>
      <td class="muted">${esc(p.email)}</td>
      <td>${cella}</td>
      <td>${p.owner ? '<span class="faint">—</span>' : comando({ permesso: R.permessi.gestisciUtentiApp, etichetta: 'Rimuovi accesso', variante: 'danger', piccolo: true, motivo: 'Serve il ruolo Admin su questa applicazione.' })}</td>
    </tr>`;
  }).join('');

  const aggiungi = comando({
    permesso: R.permessi.gestisciUtentiApp, etichetta: 'Aggiungi utente', onClick: 'aggiungi',
    motivo: 'Serve il ruolo Admin su questa applicazione: puoi vedere chi ha accesso, non cambiarlo.',
  });

  /* Il confine economico del modello, reso visibile: chi è admin abilita, non invita. */
  const notaInvito = puoGestire ? `
    <div class="notice ${R.platformRole === 'owner' ? 'notice-info' : 'notice-accent'}">
      <div>${R.platformRole === 'owner'
        ? 'Puoi abilitare qualunque persona del gruppo di lavoro. Per farne entrare una nuova, vai in <a href="#members">Members</a>: ogni posto in più si paga.'
        : 'Puoi abilitare le persone <strong>già presenti</strong> nel gruppo di lavoro. Le persone <strong>nuove</strong> le invita il titolare dell\'account, perché ogni posto in più ha un costo.'}</div>
    </div>` : '';

  return `
    <div class="spread">
      <div><h1 class="page-title">${app.nome} — Utenti</h1>
        <p class="page-sub">Chi può usare questa applicazione, e con quale ruolo.</p></div>
      ${aggiungi}
    </div>
    ${notaInvito}
    <div class="card"><div class="card-body">
      <table class="ag">
        <thead><tr><th>Nome</th><th>Email</th><th>Ruolo su ${esc(app.nome)}</th><th>Azioni</th></tr></thead>
        <tbody>${righe}</tbody>
      </table>
    </div></div>`;
}

function schermataCatalogo() {
  const schede = CATALOGO.map((a) => {
    const haAccesso = R.appVisibili.includes(a.id);
    let azione;
    if (a.stato === 'attiva' && haAccesso) {
      azione = `<button class="btn btn-primary btn-sm" data-vai="${a.id}-${(APPLICAZIONI.find((x) => x.id === a.id) || { sezioni: [{ id: '' }] }).sezioni[0].id}">Apri</button>`;
    } else if (a.stato === 'attiva' && !haAccesso) {
      azione = `<button class="btn btn-secondary btn-sm" data-azione="chiedi-abilitazione:${a.id}">Chiedi l'abilitazione</button>
        <span class="why">Attiva per l'account, ma non ti è stata abilitata.</span>`;
    } else if (R.permessi.installaApp === 'si') {
      azione = `<button class="btn btn-primary btn-sm">Attiva</button>`;
    } else if (stato.richiesteInviate[a.id]) {
      azione = `<button class="btn btn-secondary btn-sm" aria-disabled="true">Già richiesto oggi</button>
        <span class="why">Il titolare dell'account è stato avvisato.</span>`;
    } else {
      azione = `<button class="btn btn-secondary btn-sm" data-azione="chiedi-installazione:${a.id}">Chiedi all'owner di installarla</button>`;
    }
    return `<div class="card"><div class="card-body">
      <div class="spread" style="margin-bottom:8px"><strong style="font-size:15px">${esc(a.nome)}</strong>
        ${a.stato === 'attiva' ? '<span class="badge badge-success">Attiva</span>' : `<span class="badge badge-neutral">${esc(a.prezzo)}</span>`}</div>
      <p class="muted" style="margin:0 0 12px;font-size:13px">${esc(a.tagline)}</p>
      <div class="kv">${azione}</div>
    </div></div>`;
  }).join('');

  return `
    <div><h1 class="page-title">App catalog</h1>
      <p class="page-sub">${R.permessi.installaApp === 'si' ? 'Le applicazioni disponibili per il tuo workspace.' : 'Puoi guardare tutto. Per attivare un\'applicazione serve il titolare dell\'account.'}</p></div>
    <div class="grid grid-3">${schede}</div>`;
}

function schermataPrivacy() {
  const propri = `
    <div class="card"><div class="card-head"><h2>Il tuo profilo</h2></div>
      <div class="card-body"><div class="row"><input class="ag" value="${esc(RUOLO === 'owner' ? 'Marco Sindoni' : RUOLO === 'admin' ? 'Marta Rinaldi' : RUOLO === 'editor' ? 'Luca Ferri' : 'Sara Neri')}" aria-label="Nome visualizzato" />
      <button class="btn btn-secondary">Salva</button></div>
      <p class="why">Correggere i propri dati è un diritto di ogni persona (art. 16).</p></div></div>
    <div class="card"><div class="card-head"><h2>Scarica i tuoi dati</h2></div>
      <div class="card-body"><p class="muted" style="margin:0 0 12px;font-size:13px">Il tuo profilo in formato leggibile, subito (artt. 15 e 20).</p>
      <button class="btn btn-primary">Scarica</button></div></div>
    <div class="card"><div class="card-head"><h2>I tuoi diritti e chi contattare</h2></div>
      <div class="card-body"><p class="muted" style="margin:0;font-size:13px">Limitazione, opposizione, decisioni automatizzate, informativa e contatto per la protezione dei dati.</p></div></div>`;

  const account = `
    <div class="card"><div class="card-head"><h2>Esporta tutto l'account</h2></div>
      <div class="card-body"><p class="muted" style="margin:0 0 12px;font-size:13px">Tutti i dati del workspace, comprese le applicazioni.</p>
      <button class="btn btn-secondary">Avvia l'esportazione</button></div></div>
    <div class="card"><div class="card-head"><h2>Recesso e chiusura dell'account</h2></div>
      <div class="card-body"><div class="row"><button class="btn btn-secondary">Recedi da un'applicazione</button>
      <button class="btn btn-danger">Elimina l'account</button></div></div></div>`;

  const notaRidotta = R.privacyRidotta ? `
    <div class="notice notice-info">
      <div>Qui trovi i dati che riguardano <strong>te</strong>. I dati dell'account e la sua chiusura sono del
      titolare dell'account. Se vuoi che i tuoi dati siano cancellati, la richiesta va rivolta a lui; il
      contatto per la protezione dei dati di appgrove resta comunque a tua disposizione.</div>
    </div>` : '';

  return `
    <div><h1 class="page-title">I miei dati</h1>
      <p class="page-sub">${R.privacyRidotta ? 'I tuoi diritti sui tuoi dati personali.' : 'I tuoi diritti e quelli dell\'account.'}</p></div>
    ${notaRidotta}
    ${propri}
    ${R.privacyRidotta ? '' : account}`;
}

function schermataBilling() {
  return `
    <div><h1 class="page-title">Billing</h1><p class="page-sub">Abbonamenti e pagamenti del workspace.</p></div>
    <!-- Scheda dei posti (UC 0106): in testa, perché riguarda tutto l'account e non una applicazione. -->
    <div class="card"><div class="card-head"><h2>Persone del gruppo di lavoro</h2>
      <span class="badge badge-neutral">non è un'applicazione</span></div>
      <div class="card-body">
        <div class="spread">
          <div class="kv"><span class="kv-value mono">${POSTI.dovuto} / mese</span>
            <span class="kv-label mono">${POSTI.calcolo} = ${POSTI.dovuto}</span></div>
          <div class="kv"><span class="kv-value mono">${POSTI.usati}</span><span class="kv-label">posti · fascia ${POSTI.fascia}</span></div>
          <div class="kv"><span class="kv-value mono">1 set 2026</span><span class="kv-label">prossimo rinnovo</span></div>
          <button class="btn btn-secondary btn-sm" data-vai="members">Gestisci le persone</button>
        </div>
      </div></div>
    <div class="card"><div class="card-head"><h2>Abbonamenti alle applicazioni</h2></div>
      <div class="card-body"><table class="ag">
        <thead><tr><th>Applicazione</th><th>Piano</th><th>Prezzo</th><th>Rinnovo</th></tr></thead>
        <tbody>
          <tr><td><strong>Mini-CRM</strong></td><td>Team</td><td class="mono">19,00 €</td><td class="muted">1 set 2026</td></tr>
          <tr><td><strong>Fatture</strong></td><td>Pro</td><td class="mono">12,00 €</td><td class="muted">1 set 2026</td></tr>
        </tbody></table>
        <p class="why" style="margin-top:12px">La voce dei posti non compare in questa tabella: non è un'applicazione (UC 0103).</p>
      </div></div>`;
}

/*
 * Impostazioni: esiste già nel prodotto e **non cambia** con questa epica. È nel prototipo per una
 * ragione precisa — un prototipo che la omette invita a dimenticarla, e toglierla ai collaboratori
 * sarebbe una regressione: nome visualizzato e iscrizione alle notizie sono preferenze della persona,
 * non atti sull'account.
 */
function schermataImpostazioni() {
  return `<div><h1 class="page-title">Impostazioni</h1>
      <p class="page-sub">Preferenze della persona. Visibili a ogni ruolo, owner compreso: non cambiano con questa epica.</p></div>
    <div class="card"><div class="card-body">
      <p class="kv-label" style="margin:0 0 10px">Nome visualizzato</p>
      <input class="input" value="${esc((PERSONE.find((x) => (RUOLO === 'owner' ? x.owner : true)) || {}).nome || '')}" aria-label="Nome visualizzato">
      <div class="row" style="margin-top:12px"><button class="btn btn-primary btn-sm">Salva</button></div>
    </div></div>
    <div class="card"><div class="card-body spread">
      <div class="kv"><strong style="font-size:14px">Ricevi le notizie di appgrove</strong>
        <span class="muted" style="font-size:13px">Novità su applicazioni e funzioni. Si può disattivare in ogni momento.</span></div>
      <span class="badge badge-neutral">disattivate</span>
    </div></div>
    <div class="notice notice-info"><div><strong>Perché sta nel prototipo.</strong> Non cambia con questa epica, ma
    un prototipo che la omette invita a perderla per strada: queste preferenze sono <strong>della persona</strong> e
    restano visibili a ogni ruolo, come «I miei dati» (UC 0110).</div></div>`;
}

function schermataSemplice(titolo, testo) {
  return `<div><h1 class="page-title">${esc(titolo)}</h1><p class="page-sub">${esc(testo)}</p></div>
    <div class="card"><div class="card-body"><p class="muted" style="margin:0">Schermata non sviluppata nel prototipo: non cambia con questa epica.</p></div></div>`;
}

/* ── Pannello «cosa cambia per questo ruolo» ────────────────────────────────
 *
 * IMPALCATURA DEL PROTOTIPO — **non** è una schermata del prodotto e non va implementata.
 * Come la barra in alto (renderBarra), esiste solo per far leggere il prototipo: elenca le
 * differenze del ruolo e la matrice dei menu. Nel prodotto non c'è nulla di simile: le differenze
 * fra ruoli si *vivono*, non si spiegano in un riquadro.
 *
 * Convenzione: tutto ciò che è impalcatura porta classi con prefisso `x-proto-scaffold-`, che nel
 * design system non esiste. Se una di quelle classi comparisse in `frontend/`, sarebbe un errore.
 */

function renderPannello() {
  if (!stato.pannelloAperto) {
    return `<button class="x-proto-scaffold-panel-toggle" data-azione="pannello">Cosa cambia per ${esc(R.etichetta)} ▲</button>`;
  }
  const voci = R.differenze.map((d) => `<li>${d}</li>`).join('');
  const matrice = Object.entries(R.nav).map(([k, v]) =>
    `<tr><td>${k}</td><td>${v === 'si' ? '<span class="badge badge-success">visibile</span>' : v === 'sola-lettura' ? '<span class="badge badge-warning">sola lettura</span>' : '<span class="badge badge-neutral">assente</span>'}</td></tr>`).join('');
  return `<div class="x-proto-scaffold-panel">
    <div class="x-proto-scaffold-panel-head">
      <span class="kv" style="min-width:0">
        <strong>${esc(R.etichetta)}</strong>
        <span class="faint" style="font-size:11px">impalcatura del prototipo · non è prodotto</span>
      </span>
      <button class="btn btn-ghost btn-sm" style="margin-left:auto" data-azione="pannello">Chiudi</button>
    </div>
    <div class="x-proto-scaffold-panel-body">
      <p class="muted" style="margin:0">${esc(R.descrizione)}</p>
      <div><strong>Cosa cambia per questo ruolo</strong><ul>${voci}</ul></div>
      <div><strong>Voci di menu</strong>
        <table class="ag" style="font-size:12px">${matrice}</table></div>
      <p class="why" style="margin:0"><strong>Questo riquadro e la barra in alto non vanno implementati</strong>:
      sono impalcatura del prototipo (classi <code>x-proto-scaffold-*</code>). Dati inventati, nessuna
      chiamata di rete. Che cosa è prodotto e che cosa no: README.md §0.</p>
    </div>
  </div>`;
}

/* ── Barra superiore: selettore dell'account e inviti ricevuti ─────────────
 *
 * Due elementi nuovi rispetto all'interfaccia di oggi, entrambi della sotto-epica E22.5:
 *
 * 1. il SELETTORE dell'account attivo (UC 0117). Regola portante: se le appartenenze sono una sola
 *    il selettore **non viene reso affatto** — non «reso disabilitato». È lo stesso principio dei
 *    menu assenti per i collaboratori: un comando che non serve a nulla è rumore.
 * 2. gli INVITI RICEVUTI (UC 0118). Chi è già dentro la piattaforma non deve rifare la
 *    registrazione: l'invito diventa un consenso da dare qui.
 *
 * Il nome dell'account attivo è mostrato **sempre**, anche con una appartenenza sola: con più
 * account è un elemento di sicurezza percepita, non un ornamento — deve essere sempre chiaro per
 * conto di chi si sta lavorando.
 */

function renderSelettore() {
  if (MIE_APPARTENENZE.length < 2) {
    /* Una sola appartenenza: nessun comando, ma il NOME resta — è il contesto in cui si lavora. */
    return `<div class="account-fixed">
      <strong>${esc(APPARTENENZA_ATTIVA.nome)}</strong>
      <span class="faint" style="font-size:11px">${APPARTENENZA_ATTIVA.ruolo === 'owner' ? 'Sei il titolare' : 'Il tuo account'}</span>
    </div>`;
  }
  const voci = MIE_APPARTENENZE.map((a) => `
    <button class="switch-item" ${a.attiva ? 'aria-current="true"' : ''} data-azione="cambia-account:${a.id}">
      <span class="kv" style="min-width:0">
        <strong style="font-size:13px">${esc(a.nome)}</strong>
        <span class="faint" style="font-size:11px">${a.ruolo === 'owner' ? 'Sei il titolare' : 'Sei collaboratore'}</span>
      </span>
      ${a.attiva ? '<span class="badge badge-success">attivo</span>' : ''}
    </button>`).join('');
  return `<div class="switcher">
    <button class="switch-btn" data-azione="selettore" aria-expanded="${stato.selettoreAperto}">
      <span class="kv" style="min-width:0">
        <strong>${esc(APPARTENENZA_ATTIVA.nome)}</strong>
        <span class="faint" style="font-size:11px">${MIE_APPARTENENZE.length} account · cambia</span>
      </span>
      <span class="material-symbols-rounded" style="margin-left:auto;opacity:.6;font-size:18px" aria-hidden="true">expand_more</span>
    </button>
    ${stato.selettoreAperto ? `<div class="switch-menu">
      <p class="nav-label" style="padding:9px 12px 4px">I tuoi account</p>
      ${voci}
      <p class="why" style="padding:8px 12px 10px;margin:0">Cambiare account riscrive l'account attivo, rinnova il
      token e ricarica l'applicazione: è un cambio di sessione, non un filtro di visualizzazione (UC 0117).</p>
    </div>` : ''}
  </div>`;
}

/*
 * Sezione del CRUSCOTTO, non un menu dell'intestazione (UC 0118). Un invito a collaborare con
 * un'altra azienda merita una decisione consapevole: come pulsantino in alto passava inosservato, e un
 * invito non risposto non è un dettaglio — è un rapporto di lavoro in sospeso. Chi ha inviti li trova
 * in cima al cruscotto, prima delle applicazioni.
 */
function sezioneInviti() {
  if (!MIEI_INVITI.length) return '';
  const schede = MIEI_INVITI.map((i) => `
    <div class="card invite-card"><div class="card-body">
      <div class="spread" style="align-items:flex-start">
        <div class="kv">
          <span class="badge badge-accent" style="align-self:flex-start">Invito a collaborare</span>
          <strong style="font-size:16px;margin-top:8px">${esc(i.account)}</strong>
          <span class="muted" style="font-size:13px">${esc(i.da)} ti invita ad accedere · ${esc(i.quando)}</span>
        </div>
        <div class="row">
          <button class="btn btn-primary btn-sm" data-azione="accetta-invito">Accetta</button>
          <button class="btn btn-ghost btn-sm" data-azione="rifiuta-invito">Rifiuta</button>
        </div>
      </div>
      <p class="why" style="margin:12px 0 0">Sei già dentro la piattaforma: accettando non rifai la registrazione e
      non crei una seconda parola d'accesso — nasce solo una nuova <strong>appartenenza</strong> per la tua identità
      (UC 0118). L'account compare poi nel selettore, in alto a sinistra.</p>
    </div></div>`).join('');
  return `<div>
    <h2 class="section-title">${MIEI_INVITI.length === 1 ? 'Hai un invito in attesa' : `Hai ${MIEI_INVITI.length} inviti in attesa`}</h2>
    <div class="grid" style="margin-top:12px">${schede}</div>
  </div>`;
}

/*
 * Invito ad aprire un proprio account, per chi collabora soltanto negli account di altri (UC 0108 §4.5).
 *
 * Due proprietà lo rendono corretto e non fastidioso, e vanno implementate entrambe:
 *   - la condizione è «nessuna appartenenza con ruolo owner», non «sono un collaboratore qui»: chi ha
 *     già un proprio account non deve vederlo mai, nemmeno mentre lavora nell'account di un altro;
 *   - è **chiudibile**, con due orologi (UC 0108 §4.5): l'invito vive **un anno** dalla nascita dell'identità,
 *     e ogni «Non ora» lo rinvia di **una settimana**. Un invito commerciale che torna a ogni accesso si
 *     smette di leggere; uno che sparisce per sempre al primo rinvio spreca l'unica occasione di dirlo.
 *     Nel prototipo il rinvio dura fino al ricaricamento della pagina: i due orologi non sono simulati.
 *
 * Non dice nulla a chi ospita: l'avviso è dentro la sessione della persona, e l'owner dell'account
 * ospitante non ha modo di sapere che le è stato mostrato.
 */
function sezioneAccountProprio() {
  if (!SENZA_ACCOUNT_PROPRIO || stato.invitoAccountChiuso) return '';
  const ospitanti = MIE_APPARTENENZE.map((a) => a.nome);
  const elenco = ospitanti.length === 1
    ? `<strong>${esc(ospitanti[0])}</strong>`
    : ospitanti.slice(0, -1).map((n) => `<strong>${esc(n)}</strong>`).join(', ')
      + ` e <strong>${esc(ospitanti[ospitanti.length - 1])}</strong>`;
  return `<div class="notice notice-accent" style="padding:17px 19px">
    <span class="avatar" style="border-radius:10px;background:rgb(var(--ag-accent) / .16);color:rgb(var(--ag-accent))" aria-hidden="true"><span class="material-symbols-rounded" style="font-variation-settings:'FILL' 1">add_business</span></span>
    <div style="flex:1">
      <strong style="font-size:15px">Puoi avere anche il tuo account appgrove</strong>
      <p style="margin:6px 0 0;font-size:13px">Oggi collabori in ${elenco}, dove le applicazioni le sceglie chi
      ti ha invitato. Aprendo un account tuo decidi tu quali attivare — e <strong>non perdi nulla</strong>:
      resti dove sei, e ${MIE_APPARTENENZE.length > 1
        ? 'passi da un account all\'altro dal selettore qui a sinistra'
        : 'appena avrai due account comparirà qui a sinistra il selettore per passare dall\'uno all\'altro'}.
      I <strong>primi tre posti sono gratuiti</strong>, il tuo compreso.</p>
      <div class="row" style="margin-top:12px">
        <button class="btn btn-primary btn-sm" data-azione="apri-account">Apri il mio account</button>
        <button class="btn btn-ghost btn-sm" data-azione="chiudi-invito-account">Non ora</button>
      </div>
    </div>
  </div>`;
}

/*
 * Percorso di navigazione: è ciò che l'intestazione VERA contiene già oggi
 * (frontend/apps/backoffice/src/shell/Breadcrumb.tsx). Mostrato qui perché l'intestazione non sembri
 * vuota dopo che il selettore è passato alla barra laterale e l'etichetta di ruolo è stata rimossa.
 */
function percorso() {
  const s = stato.schermata;
  const voce = VOCI_PIATTAFORMA.find((v) => v.id === s);
  if (voce) return `<strong style="color:rgb(var(--ag-text))">${esc(voce.label)}</strong>`;
  const [appId, sezId] = s.split('-');
  const app = APPLICAZIONI.find((a) => a.id === appId);
  const sez = app && app.sezioni.find((x) => x.id === sezId);
  if (!app) return '';
  return `${esc(app.nome)} <span style="opacity:.45">›</span> <strong style="color:rgb(var(--ag-text))">${sez ? esc(sez.label) : ''}</strong>`;
}

/* ── Assemblaggio e navigazione ──────────────────────────────────────────── */

const RUOLI_ORDINE = ['owner', 'admin', 'editor', 'viewer'];

function renderBarra() {
  const link = RUOLI_ORDINE.map((r) =>
    `<a href="${r}.html#${stato.schermata}" ${r === RUOLO ? 'aria-current="true"' : ''}>${MATRICE[r].etichetta}</a>`).join('');
  /* IMPALCATURA — non è prodotto. Vedi il commento in testa a renderPannello(). */
  return `<div class="x-proto-scaffold-bar">
    <strong>Prototipo · epica 22</strong>
    <span class="x-proto-scaffold-tag" title="Questa barra è impalcatura del prototipo: non fa parte del prodotto e non va implementata. Serve solo a passare da un ruolo all'altro mantenendo la schermata.">non è prodotto</span>
    <span style="opacity:.6">vista da:</span> ${link}
    <a href="platform-admin.html" style="margin-left:auto">Console appgrove →</a></div>`;
}

function contenuto() {
  const s = stato.schermata;
  if (s === 'dashboard') return schermataDashboard();
  if (s === 'members') return R.nav.members === 'si' ? schermataMembers() : schermataVietata();
  if (s === 'billing') return R.nav.billing === 'si' ? schermataBilling() : schermataVietata();
  if (s === 'account') return R.nav.account === 'si' ? schermataSemplice('Account', 'Dati del workspace e identificativo.') : schermataVietata();
  if (s === 'catalog') return schermataCatalogo();
  if (s === 'privacy') return schermataPrivacy();
  if (s === 'support') return schermataSemplice('Supporto', 'Apri una richiesta di assistenza.');
  if (s === 'settings') return schermataImpostazioni();
  if (s === 'security') return schermataSemplice('Sicurezza', 'Secondo fattore di autenticazione e sessioni. Resta di ogni persona, in ogni ruolo.');
  const [appId, sez] = s.split('-');
  if (!R.appVisibili.includes(appId)) return schermataVietata();
  if (sez === 'utenti') return schermataUtentiApp(appId);
  if (appId === 'fatture') return schermataSemplice('Fatture — Documenti', 'Non sviluppata nel prototipo.');
  return schermataContatti(appId);
}

function schermataVietata() {
  return `<div><h1 class="page-title">Non hai accesso a questa pagina</h1>
    <p class="page-sub">Questa sezione è del titolare dell'account. Se ti serve, chiediglielo.</p></div>
    <div class="notice notice-warning"><div>Nel prodotto reale non ci si arriva navigando: la voce non compare nel menu e
    la guardia di rotta rimanda qui (difesa a due livelli, UC 0107). Questa schermata si vede solo digitando l'indirizzo.</div></div>`;
}

function render() {
  document.body.innerHTML = `${renderSidebar()}
    <div class="main">
      ${renderBarra()}
      <div class="topbar">
        ${percorso()}
        <!-- Nessuna etichetta di ruolo qui: il ruolo è PER APPLICAZIONE, quindi una sola etichetta
             globale sarebbe falsa appena una persona è abilitata a più di una applicazione. Il ruolo si
             legge dove ha senso: sulla scheda dell'applicazione nel cruscotto e in testa alle sue
             schermate (UC 0101). -->
        <span class="x-proto-scaffold-note">non reso nel prototipo: lingua, tema, notifiche</span>
      </div>
      <div class="content">${contenuto()}</div>
    </div>
    ${renderPannello()}`;
}

document.addEventListener('click', (e) => {
  const vai = e.target.closest('[data-vai]');
  if (vai) { stato.schermata = vai.dataset.vai; location.hash = stato.schermata; render(); return; }
  const az = e.target.closest('[data-azione]');
  if (!az) return;
  const [azione, arg] = az.dataset.azione.split(':');
  if (azione === 'pannello') stato.pannelloAperto = !stato.pannelloAperto;
  if (azione === 'selettore') stato.selettoreAperto = !stato.selettoreAperto;
  if (azione === 'cambia-account') {
    const scelta = MIE_APPARTENENZE.find((a) => a.id === arg);
    stato.selettoreAperto = false;
    if (scelta && !scelta.attiva) {
      /* Il prototipo non simula due insiemi di dati: mostra dov'è il comando e cosa fa il prodotto,
         poi porta al prototipo dell'esperienza in quell'account, quando esiste. */
      alert(`Prototipo: nel prodotto reale la scelta di «${scelta.nome}» scrive l'account attivo, rinnova il token e `
        + `ricarica l'applicazione (UC 0117). L'appartenenza viene RIVERIFICATA al momento del rinnovo: `
        + `il valore conservato non è creduto.`);
      if (scelta.prototipo) location.href = scelta.prototipo;
      return;
    }
  }
  if (azione === 'accetta-invito') { alert('Prototipo: accettando, il sistema crea una NUOVA APPARTENENZA per la tua identità esistente — nessuna seconda registrazione (UC 0118). L\'account appare nel selettore e diventa quello attivo.'); }
  if (azione === 'rifiuta-invito') { alert('Prototipo: l\'invito si chiude come rifiutato e il posto pagato torna disponibile per l\'account che invitava (UC 0118, punto aperto sul rimborso).'); }
  /* Invito ad aprire un proprio account (UC 0108 §4.5): il percorso di registrazione è quello che già
     esiste, non ne nasce uno nuovo — l'identità è la stessa e si aggiunge un'appartenenza con ruolo owner. */
  if (azione === 'apri-account') alert('Prototipo: porta al percorso di apertura di un account nuovo. La tua identità resta UNA (UC 0116): nasce una nuova appartenenza, con ruolo owner, e le appartenenze che hai negli account di altri non cambiano. I primi tre posti del tuo account sono gratuiti, il tuo compreso.');
  if (azione === 'chiudi-invito-account') {
    stato.invitoAccountChiuso = true;   // nel prodotto: rinvio di UNA SETTIMANA, dentro la finestra di UN ANNO
  }
  if (azione === 'riduci') stato.riduzioneInAttesa = true;
  if (azione === 'annulla-riduzione') stato.riduzioneInAttesa = false;
  if (azione === 'invita') alert('Prototipo: qui si aprirebbe la finestra di invito, che mostra il costo del posto PRIMA della conferma (UC 0103).');
  if (azione === 'aggiungi') alert('Prototipo: qui si sceglierebbe una persona già presente nel gruppo di lavoro e il suo ruolo su questa applicazione (UC 0111).');
  if (azione === 'chiedi-installazione') { stato.richiesteInviate[arg] = true; alert('Prototipo: richiesta inviata al titolare dell\'account per email (UC 0109).'); }
  if (azione === 'chiedi-abilitazione') alert('Prototipo: richiesta di abilitazione inviata (UC 0109).');
  if (azione === 'esci') alert('Prototipo: uscita dalla sessione. Non cambia con questa epica.');
  render();
});

window.addEventListener('hashchange', () => {
  const h = location.hash.replace('#', '');
  if (h && h !== stato.schermata) { stato.schermata = h; render(); }
});

render();

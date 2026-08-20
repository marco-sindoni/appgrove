/*
 * Prototipi dell'epica 22 — dati d'esempio e MATRICE DEI PERMESSI.
 *
 * Questo file è la parte che conta per chi implementerà: la costante MATRICE è la specifica
 * «ruolo × elemento» in forma leggibile. Chi scrive il codice la traduce in guardie di rotta e
 * condizioni di visibilità; chi collauda la traduce in casi di prova.
 *
 * I dati sono inventati. Nessuna chiamata di rete: i prototipi non parlano con nulla.
 */

/* ── Dati d'esempio ───────────────────────────────────────────────────────── */

const ACCOUNT = { nome: 'Studio Marchetti' };

/* ── Appartenenze della persona che sta guardando (UC 0116/0117) ────────────
 * Una persona può appartenere a PIÙ account: l'identità è di piattaforma, l'appartenenza è
 * dell'account. Il selettore compare SOLO quando le appartenenze sono più di una — con una sola
 * non esiste affatto, che è il caso di tutti gli utenti di oggi.
 *
 * `prototipo` indica il file che mostra l'esperienza in quell'altro account, quando esiste: serve a
 * rendere il cambio *navigabile* invece che soltanto descritto.
 */
const APPARTENENZE = {
  owner:  [{ id: 'sm', nome: 'Studio Marchetti', ruolo: 'owner', attiva: true }],
  admin:  [{ id: 'sm', nome: 'Studio Marchetti', ruolo: 'member', attiva: true },
           { id: 'rd', nome: 'Rinaldi Design', ruolo: 'owner', attiva: false, prototipo: 'owner.html' }],
  editor: [{ id: 'sm', nome: 'Studio Marchetti', ruolo: 'member', attiva: true }],
  viewer: [{ id: 'sm', nome: 'Studio Marchetti', ruolo: 'member', attiva: true },
           { id: 'gt', nome: 'Greco Trasporti', ruolo: 'member', attiva: false, prototipo: 'viewer.html' }],
};

/* Inviti in attesa di risposta, visti da chi li riceve (UC 0118 §4.4). Chi è già dentro la
   piattaforma non deve rifare la registrazione: l'invito diventa un consenso da dare qui. */
const INVITI_RICEVUTI = {
  owner: [],
  admin: [{ id: 'i1', account: 'Greco Trasporti', da: 'Fabio Greco', quando: '2 giorni fa' }],
  editor: [],
  viewer: [],
};

/* Le persone dell'account. NB: nessun «ruolo» a questo livello — è il punto dell'epica.
   `accessi` è la traduzione dell'entità platform.app_access (UC 0098). */
const PERSONE = [
  { id: 'u1', nome: 'Marco Sindoni', email: 'marco@studiomarchetti.it', stato: 'attiva', owner: true,
    dal: '12 gen 2026', accessi: {} },
  { id: 'u2', nome: 'Marta Rinaldi', email: 'marta@studiomarchetti.it', stato: 'attiva', owner: false,
    dal: '3 mar 2026', accessi: { crm: 'admin', fatture: 'editor' } },
  { id: 'u3', nome: 'Luca Ferri', email: 'luca@studiomarchetti.it', stato: 'attiva', owner: false,
    dal: '18 apr 2026', accessi: { crm: 'editor' } },
  { id: 'u4', nome: 'Sara Neri', email: 'sara@studiomarchetti.it', stato: 'attiva', owner: false,
    dal: '2 giu 2026', accessi: { crm: 'viewer' } },
  { id: 'u5', nome: 'Giulia Bo', email: 'giulia@studiomarchetti.it', stato: 'invito', owner: false,
    dal: '14 ago 2026', accessi: {} },
];

/* Le applicazioni a cui l'ACCOUNT ha diritto (entitlement). L'accesso della PERSONA è un'altra cosa. */
/* `icona` e le icone delle sezioni sono i nomi VERI dei manifesti dei moduli
   (frontend/apps/backoffice/src/modules/{crm,fatture}/manifest.ts): non inventarli qui, o il prototipo
   mostra un'interfaccia che non esiste. */
const APPLICAZIONI = [
  { id: 'crm', nome: 'Mini-CRM', icona: 'contacts', tinta: 'cat-blue',
    sezioni: [{ id: 'contatti', label: 'Contatti', icona: 'contacts' }, { id: 'utenti', label: 'Utenti', icona: 'group' }] },
  { id: 'fatture', nome: 'Fatture', icona: 'receipt_long', tinta: 'cat-green',
    sezioni: [{ id: 'documenti', label: 'Documenti', icona: 'receipt_long' }, { id: 'utenti', label: 'Utenti', icona: 'group' }] },
];

const CONTATTI = [
  { nome: 'Alessandro Bianchi', azienda: 'Bianchi Serramenti', fase: 'trattativa', valore: '4.200 €' },
  { nome: 'Chiara De Santis', azienda: 'DS Consulting', fase: 'proposta', valore: '11.500 €' },
  { nome: 'Fabio Greco', azienda: 'Greco Trasporti', fase: 'contatto', valore: '2.800 €' },
  { nome: 'Elena Rossi', azienda: 'Rossi & Figli', fase: 'vinta', valore: '7.900 €' },
];

const CATALOGO = [
  { id: 'crm', nome: 'Mini-CRM', tagline: 'Contatti, trattative e una pipeline semplice.', prezzo: '19 €/mese', stato: 'attiva' },
  { id: 'fatture', nome: 'Fatture', tagline: 'Fatturazione elettronica senza pensieri.', prezzo: '12 €/mese', stato: 'attiva' },
  { id: 'agenda', nome: 'Agenda', tagline: 'Appuntamenti e prenotazioni online.', prezzo: '9 €/mese', stato: 'disponibile' },
  { id: 'note', nome: 'Note', tagline: 'Appunti condivisi e mappe mentali.', prezzo: '7 €/mese', stato: 'disponibile' },
];

/* Posti (UC 0102/0103): 4 persone attive + 1 invito in attesa = 5 posti, 3 gratuiti.
   Listino a SCAGLIONI PROGRESSIVI: ogni posto paga la tariffa della sua fascia.
   Con 5 posti → 3 gratuiti + 2 posti nella fascia 4–10 a 2,99 = 5,98 €.
   Il sesto posto costa 2,99 (siamo dentro la fascia) e porta il dovuto a 8,97 €. */
const POSTI = {
  usati: 5, attive: 4, inviti: 1, inCessazione: 0,
  gratuiti: 3, fascia: '4–10 posti', tariffa: '2,99 €',
  dovuto: '5,98 €', prossimo: '2,99 €', dovutoDopo: '8,97 €',
  /* Il calcolo scritto per esteso: è la somma degli scaglioni, e va mostrata così — un prodotto unico
     («(5 − 3) × 2,99») tornerebbe per caso con cinque posti e sarebbe falso con cinquanta. */
  calcolo: '3 gratuiti + 2 × 2,99 €',
  spesaTotale: '36,98 €',   /* 19,00 Mini-CRM + 12,00 Fatture + 5,98 posti */
};

/* Le fasce del listino, come le vede l'amministratore di piattaforma (UC 0102/0105). */
const FASCE = [
  { da: 1, a: 3, tariffa: '0,00', nota: 'franchigia — owner incluso' },
  { da: 4, a: 10, tariffa: '2,99', nota: '' },
  { da: 11, a: 50, tariffa: '1,99', nota: '' },
  { da: 51, a: 100, tariffa: '0,99', nota: '' },
  { da: 101, a: null, tariffa: '0,49', nota: 'oltre 100' },
];

/* ─────────────────────────────────────────────────────────────────────────────
 * MATRICE DEI PERMESSI — la specifica.
 *
 * Tre valori possibili per ogni elemento:
 *   'si'          → visibile e utilizzabile
 *   'sola-lettura'→ visibile, non utilizzabile: comando DISABILITATO con spiegazione (UC 0101)
 *   'no'          → ASSENTE dalla navigazione (ambito che non compete al ruolo)
 *
 * La differenza fra 'sola-lettura' e 'no' non è estetica: sono due meccanismi diversi da
 * implementare, ed è la regola portante di E22.3.
 * ───────────────────────────────────────────────────────────────────────────── */
const MATRICE = {
  owner: {
    etichetta: 'Owner', descrizione: 'Titolare dell\'account: ha creato il workspace appgrove.',
    platformRole: 'owner',
    appRoles: { crm: 'admin', fatture: 'admin' },   // implicito: l'owner non ha righe di accesso
    appVisibili: ['crm', 'fatture'],
    nav: { dashboard: 'si', catalog: 'si', account: 'si', billing: 'si', members: 'si', privacy: 'si', support: 'si', settings: 'si', security: 'si' },
    privacyRidotta: false,
    permessi: {
      invitaPersone: 'si',            // solo l'owner: l'invito ha effetto economico (UC 0103)
      riduciPosti: 'si',
      installaApp: 'si',
      chiediInstallazione: 'no',      // non ne ha bisogno: installa
      gestisciPiano: 'si',
      creaContatto: 'si', modificaContatto: 'si', eliminaContatto: 'si',
      gestisciUtentiApp: 'si', cambiaRuoli: 'si',
      esportaAccount: 'si',
    },
    differenze: [
      'Vede il menu completo: <strong>Account</strong>, <strong>Billing</strong> e <strong>Members</strong> sono solo suoi.',
      'È l\'unico che può <strong>invitare persone nuove</strong>, perché ogni posto in più si paga.',
      'In «Members» vede il <strong>riquadro dei posti</strong> con il costo e può avviare la riduzione in attesa.',
      'Ha accesso implicito a <strong>tutte</strong> le applicazioni dell\'account, senza righe di accesso.',
      'In ogni applicazione può cambiare i ruoli di chiunque.',
      'Appartiene a <strong>un solo account</strong>: nessun selettore in alto — con una appartenenza sola non esiste (UC 0117).',
    ],
  },

  admin: {
    etichetta: 'Admin del Mini-CRM', descrizione: 'Collaboratore con ruolo admin sul Mini-CRM.',
    platformRole: 'member',
    appRoles: { crm: 'admin' },
    appVisibili: ['crm'],
    /* Impostazioni e sicurezza sono PREFERENZE DELLA PERSONA (nome visualizzato, notizie, secondo
       fattore): restano visibili a tutti, come «I miei dati». Toglierle ai collaboratori sarebbe una
       regressione — sono sue, non dell'account. */
    nav: { dashboard: 'si', catalog: 'si', account: 'no', billing: 'no', members: 'no', privacy: 'si', support: 'si', settings: 'si', security: 'si' },
    privacyRidotta: true,
    permessi: {
      invitaPersone: 'no',            // il confine economico del modello
      riduciPosti: 'no',
      installaApp: 'no',
      chiediInstallazione: 'si',
      gestisciPiano: 'no',
      creaContatto: 'si', modificaContatto: 'si', eliminaContatto: 'si',
      gestisciUtentiApp: 'si', cambiaRuoli: 'si',
      esportaAccount: 'no',
    },
    differenze: [
      '<strong>Non</strong> vede Account, Billing e Members: sono ambiti del titolare dell\'account.',
      'Vede solo il <strong>Mini-CRM</strong>, l\'unica applicazione a cui è stato abilitato: Fatture non esiste per lui.',
      'Dentro il Mini-CRM può <strong>abilitare altre persone dell\'account</strong> e cambiarne i ruoli.',
      'Non può <strong>invitare persone nuove</strong>: la richiesta rimanda al titolare, perché costa.',
      '«I miei dati» è in <strong>forma ridotta</strong>: solo i propri diritti, nessun atto sull\'account.',
      'Appartiene a <strong>due account</strong>: in alto compare il <strong>selettore</strong> — qui è collaboratrice, nel suo studio è titolare (UC 0117).',
      'Ha un <strong>invito in attesa</strong> da una terza azienda: essendo già dentro la piattaforma, lo accetta da qui senza registrarsi di nuovo (UC 0118).',
    ],
  },

  editor: {
    etichetta: 'Editor del Mini-CRM', descrizione: 'Collaboratore con ruolo editor sul Mini-CRM.',
    platformRole: 'member',
    appRoles: { crm: 'editor' },
    appVisibili: ['crm'],
    /* Impostazioni e sicurezza sono PREFERENZE DELLA PERSONA (nome visualizzato, notizie, secondo
       fattore): restano visibili a tutti, come «I miei dati». Toglierle ai collaboratori sarebbe una
       regressione — sono sue, non dell'account. */
    nav: { dashboard: 'si', catalog: 'si', account: 'no', billing: 'no', members: 'no', privacy: 'si', support: 'si', settings: 'si', security: 'si' },
    privacyRidotta: true,
    permessi: {
      invitaPersone: 'no', riduciPosti: 'no', installaApp: 'no', chiediInstallazione: 'si',
      gestisciPiano: 'no',
      creaContatto: 'si', modificaContatto: 'si', eliminaContatto: 'si',
      gestisciUtentiApp: 'sola-lettura', cambiaRuoli: 'sola-lettura',
      esportaAccount: 'no',
    },
    differenze: [
      'Fa <strong>tutto</strong> quello che l\'applicazione prevede: crea, modifica ed elimina contatti.',
      'La schermata <strong>Utenti</strong> la vede, ma in <strong>sola lettura</strong>: sa chi ha accesso, non lo cambia.',
      'I comandi che non gli competono sono <strong>disabilitati con la spiegazione</strong>, non nascosti: la funzione esiste, gli manca il ruolo.',
      'Come ogni collaboratore, non vede Account, Billing e Members.',
      'Appartiene a <strong>un solo account</strong>: nessun selettore. Il confronto con Admin e Viewer mostra che il selettore dipende dalle <em>appartenenze</em>, non dal ruolo.',
    ],
  },

  viewer: {
    etichetta: 'Viewer del Mini-CRM', descrizione: 'Collaboratore con ruolo viewer sul Mini-CRM.',
    platformRole: 'member',
    appRoles: { crm: 'viewer' },
    appVisibili: ['crm'],
    /* Impostazioni e sicurezza sono PREFERENZE DELLA PERSONA (nome visualizzato, notizie, secondo
       fattore): restano visibili a tutti, come «I miei dati». Toglierle ai collaboratori sarebbe una
       regressione — sono sue, non dell'account. */
    nav: { dashboard: 'si', catalog: 'si', account: 'no', billing: 'no', members: 'no', privacy: 'si', support: 'si', settings: 'si', security: 'si' },
    privacyRidotta: true,
    permessi: {
      invitaPersone: 'no', riduciPosti: 'no', installaApp: 'no', chiediInstallazione: 'si',
      gestisciPiano: 'no',
      creaContatto: 'sola-lettura', modificaContatto: 'sola-lettura', eliminaContatto: 'sola-lettura',
      gestisciUtentiApp: 'sola-lettura', cambiaRuoli: 'sola-lettura',
      esportaAccount: 'no',
    },
    differenze: [
      'Vede <strong>tutti i dati</strong> dell\'applicazione — il requisito è esplicito: nessun dato gli è nascosto.',
      'Non può compiere <strong>alcuna</strong> operazione dispositiva: crea, modifica ed elimina sono disabilitati con spiegazione.',
      'Può però <strong>scaricare i propri dati</strong> da «I miei dati»: è un diritto della persona, esente da ogni ruolo.',
      'Come ogni collaboratore, vede solo le applicazioni a cui è abilitato.',
      'Appartiene a <strong>due account</strong>, quindi ha il <strong>selettore</strong> pur essendo il ruolo con meno poteri: le due cose sono indipendenti (UC 0117).',
    ],
  },
};

/* Etichette dei ruoli come le legge una persona, non come le scrive il codice. */
const RUOLO_UMANO = {
  admin: 'può gestire gli utenti',
  editor: 'può modificare',
  viewer: 'può consultare',
};

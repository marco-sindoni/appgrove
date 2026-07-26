# UC 0085 — Unificazione in services/commons dei renderer dei template email

**Area**: 17-skill-e-tooling-contenuto · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0018 (email di autenticazione via SES), UC 0039 (newsletter con email di conferma)
**Fonte**: R17 (Tabella residui in docs/usecases/_INDEX.md); docs/_BACKLOG.md §Tooling ("unificare in services/commons i due renderer Java dei template email")
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope

Eliminare una **duplicazione tecnica**: oggi la logica Java che rende le email — risolve le stringhe della lingua e
riempie l'impaginazione condivisa — esiste in **due copie quasi identiche**:
- `EmailTemplates` in `services/auth` (email di autenticazione: verifica, reimpostazione password, invito — UC 0018);
- `NewsletterEmailRenderer` in `services/core` (email di conferma iscrizione newsletter — UC 0039), definito nel suo
  stesso codice come "il gemello compatto" del primo.

I **testi** restano già a sorgente unica in `shared/email-templates` (cartella con `layout.html`, `layout.txt`,
`en.json`, `it.json`, …), copiata nell'artefatto di ciascun servizio a tempo di build: quello va bene e non si tocca.
Il problema è solo la **logica di resa**, duplicata: due copie che possono divergere in silenzio (una corregge un
escape, l'altra no; una aggiunge una lingua, l'altra resta indietro).

**Obiettivo**: estrarre un **unico renderer** in `services/commons` — con **set di lingue parametrizzabile** — usato
da entrambi i servizi. È un **refactor tecnico a comportamento invariato**: nessuna email cambia contenuto o resa.

**Incluso**: la classe renderer condivisa in `services/commons`; l'adeguamento di `services/auth` e `services/core`
per usarla; i test spostati/adeguati.
**Escluso**: modifiche ai testi (`shared/email-templates`); il Custom Message Lambda in Python (UC 0018), che rende
la stessa cartella ma è un altro linguaggio e resta fuori scope; nuove lingue o nuovi tipi di messaggio.

## 2. Attori & ruoli

- **Platform Engineer / sviluppatore backend**: esegue il refactor, verifica la parità di comportamento, approva la change.
- **`services/commons`**: nuova casa del renderer condiviso (già ospita utilità trasversali: tenancy, privacy,
  entitlement, web, gdpr).
- **`services/auth` e `services/core`**: consumatori del renderer condiviso al posto delle proprie copie.
- Nessun attore esterno o runtime: è una modifica interna al codice.

## 3. Precondizioni

- Esistono e sono verdi i due renderer attuali con i rispettivi test (`EmailTemplatesTest`, `EmailTemplatesParityTest`
  in `services/auth`; i test del renderer newsletter in `services/core`).
- `services/commons` è già una dipendenza comune dei due servizi (ospita utilità condivise) — nessun nuovo modulo da
  creare, solo una classe in più.
- La sorgente unica `shared/email-templates` è invariata e già copiata nell'artefatto di ogni servizio via `pom.xml`.

## 4. Flusso principale

1. **Estrazione**: creare in `services/commons` (package `app.appgrove.commons`, area email) un renderer unico che
   incapsula i due passaggi comuni: (a) risoluzione delle stringhe della lingua contro i valori dinamici
   (segnaposto `{{...}}`); (b) riempimento dell'impaginazione condivisa (`layout.html`/`layout.txt`). Mantiene
   l'**escape** dei valori nella versione grafica e la **guardia sui segnaposto non risolti** (fallire prima di
   spedire un'email con dentro il nome di un buco). Espone il record `Rendered(subject, text, html)`.
2. **Set di lingue parametrizzabile**: il renderer condiviso riceve il set di lingue supportate come parametro
   (auth usa l'insieme pieno via `Locales.SUPPORTED`; la newsletter usa il sottoinsieme `en`/`it` con ripiego su
   `en`, convenzione UC 0018). Così la stessa classe serve entrambi senza forzare una copertura linguistica unica.
3. **Adeguamento auth**: `services/auth` usa il renderer condiviso per i messaggi `verify`/`reset`/`invite`; la sua
   `EmailTemplates` diventa un sottile adattatore o sparisce, a comportamento invariato.
4. **Adeguamento core**: `services/core` usa il renderer condiviso per il messaggio `newsletter-confirm`; il
   `NewsletterEmailRenderer` diventa un sottile adattatore o sparisce.
5. **Test**: spostare/adeguare i test in `services/commons` (resa, parità lingue, escape, guardia segnaposto) e
   verificare che i test di auth e core restino verdi. Nessun byte delle email prodotte deve cambiare.
6. **Chiusura**: refactor tecnico → `run-tests.sh backend` verde → change via `new-change` (branch + consenso).

## 5. Flussi alternativi / edge / errori

- **Divergenza pre-esistente fra le due copie**: prima di unificare, confrontare le due implementazioni riga per riga;
  se emergono differenze reali (non solo il set di lingue), la scelta va documentata in `decisions.json` — si adotta
  il comportamento corretto, non "quello di una delle due a caso".
- **Segnaposto non risolto**: il renderer condiviso mantiene la guardia esistente e lancia un errore invece di
  spedire un'email incoerente (comportamento invariato rispetto a oggi).
- **Lingua non supportata dal set richiesto**: ripiego sulla lingua di default del set (per la newsletter, `en`).
- **Escape mancante**: la versione grafica deve continuare a fare escape dei valori (es. `&` nell'indirizzo di
  verifica): un test dedicato lo protegge contro regressioni.
- **Rischio di regressione silenziosa**: essendo email verso utenti reali, il criterio di accettazione è la
  **parità byte-a-byte** dell'output rispetto ai due renderer attuali su un insieme di casi rappresentativi.

## 6. Risorse & runbook

- **File toccati**:
  - nuovo: renderer condiviso in `services/commons/src/main/java/app/appgrove/commons/...` (area email) + suoi test;
  - `services/auth/.../EmailTemplates.java` → adattatore sottile o rimosso;
  - `services/core/.../newsletter/NewsletterEmailRenderer.java` → adattatore sottile o rimosso;
  - invariati: `shared/email-templates/*` (testi + impaginazione), le configurazioni `pom.xml` di copia risorse.
- **Non c'è un generatore/skill**: è un refactor interno; lo strumento è la suite di test come rete di sicurezza.
- **Runbook**: leggere e confrontare le due copie → estrarre il renderer condiviso con set di lingue parametrizzabile
  → puntare auth e core alla classe condivisa → `run-tests.sh backend` verde → `new-change` (branch + commit + merge).
- **Rollback**: essendo a comportamento invariato e coperto da test, il rollback è il revert del branch prima del merge.

## 7. Dati toccati

**Nessuno**. È un refactor tecnico: non tocca dati personali, non crea né modifica tabelle, non apre nuovi
trattamenti; nessun manifesto dati o RoPA da aggiornare. Le email prodotte restano identiche.

## 8. Permessi & gate

- **Invarianti multi-tenancy**: non pertinenti (nessuna query tenant-scoped; il renderer è puro codice di resa).
- **Logging strutturato**: se il renderer emette log, resta coerente con lo standard (`tenant_id`, `app_id`,
  `user_id` dove disponibili nel contesto chiamante), senza introdurre log nuovi non necessari.
- **Gate di processo**: consenso al commit e al merge restano dello sviluppatore (`new-change`).

## 9. Requisiti di test

- **Unit del renderer condiviso** (in `services/commons`): resa dei messaggi `verify`/`reset`/`invite` e
  `newsletter-confirm`; parità delle lingue per il set richiesto; escape dei valori nella versione grafica; guardia
  che fallisce sui segnaposto non risolti.
- **Parità di comportamento**: confronto dell'output (subject/text/html) del renderer condiviso con quello dei due
  renderer attuali su casi rappresentativi — deve coincidere byte-a-byte.
- **Regressione servizi**: i test esistenti di `services/auth` e `services/core` restano verdi dopo il puntamento
  alla classe condivisa.
- Prima del merge: `run-tests.sh backend` verde (auth + core + commons).

## 10. Riferimenti & Definition of Done

- **Riferimenti**: UC 0018 (email di autenticazione, sorgente unica dei testi + Custom Message Lambda), UC 0039
  (newsletter, email di conferma); nota di backlog sollevata dalla change 0052; epica: 0084-skill-new-blog-post.md.
- **DoD**:
  1. Esiste un unico renderer email in `services/commons` con set di lingue parametrizzabile.
  2. `services/auth` e `services/core` lo usano; le due copie precedenti sono rimosse o ridotte a adattatori sottili.
  3. Le email prodotte sono invariate (parità di output verificata dai test).
  4. `run-tests.sh backend` verde; nessun dato personale o manifesto toccato.

## Punti aperti / decisioni differite

- **Custom Message Lambda (Python) fuori scope**: rende la stessa cartella `shared/email-templates` con gli stessi due
  passaggi, ma in un altro linguaggio; unificarlo con la logica Java non è possibile e resta fuori. Se in futuro la
  logica di resa diverge fra Java e Python, va tracciato come punto a sé (possiede il tema UC 0018).
- **Dove collocare esattamente il renderer in commons**: package e nome della classe sono un dettaglio da fissare
  all'implementazione (area email dedicata dentro `app.appgrove.commons`); da annotare in `decisions.json`.
- **Set di lingue della newsletter**: oggi `en`/`it`; se in futuro la newsletter passasse alle 5 lingue, basterà
  cambiare il parametro — la scelta di quando farlo appartiene a UC 0039, non a questo refactor.

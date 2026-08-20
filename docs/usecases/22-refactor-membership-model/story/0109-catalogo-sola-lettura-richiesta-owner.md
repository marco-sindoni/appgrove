# UC 0109 — Catalogo in sola lettura e richiesta «chiedi all'owner di installare»

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.3 Esperienza per ruolo](../epic/E22-03-esperienza-per-ruolo.md)
**Dipendenze**: UC 0107 (visibilità per ruolo), UC 0095 (pagina del catalogo), UC 0018 (email transazionali), UC 0085 (renderer condiviso delle email)
**Piano di lavoro**: [task/0109](../task/0109-catalogo-sola-lettura-richiesta-owner.md)
**Prototipo**: [editor.html](../prototype/editor.html), scheda del catalogo
**Ultimo aggiornamento**: 2026-08-19

## 1. Obiettivo / Scope

Il collaboratore vede il catalogo — sapere che cosa esiste non richiede diritti — ma **non compra**. Al
posto del comando di acquisto trova **«chiedi all'owner di installarla»**, che recapita all'owner una email
con chi ha chiesto e che cosa.

**Incluso**: la versione in sola lettura della pagina del catalogo e della scheda di dettaglio; la richiesta
di installazione con la sua email, il suo limite di frequenza e la sua traccia; ciò che l'owner vede
quando riceve la richiesta.

**Escluso**: il menu → UC 0107; il cruscotto → UC 0108; l'acquisto vero, che resta come oggi per l'owner.

## 2. Attori & ruoli

- **Collaboratore**: guarda, e chiede.
- **Owner**: riceve la richiesta e decide. Nessun obbligo, nessun automatismo.
- **Sistema**: recapita, limita la frequenza, tiene traccia.

## 3. Precondizioni

- Esiste la pagina del catalogo con le schede delle applicazioni e il loro stato (UC 0095).
- Esiste l'impianto delle email transazionali con il renderer condiviso (UC 0018, UC 0085).

## 4. Flusso principale

1. Il collaboratore apre «App catalog» e vede le stesse applicazioni che vede l'owner, con lo stesso stato
   («già attiva», «disponibile»).
2. Sulle applicazioni **già attive per l'account**:
   - se ha accesso → «Apri»;
   - se **non** ha accesso → il testo dice che l'applicazione è attiva per l'account ma lui non è
     abilitato, con il comando **«chiedi l'abilitazione»** (che è un caso diverso dall'installazione:
     l'applicazione c'è, manca l'accesso).
3. Sulle applicazioni **non attive**: al posto di «Attiva» trova **«chiedi all'owner di installarla»**.
4. Premendo il comando: una finestra chiede una **nota facoltativa** («a che ti serve?», massimo un paio di
   righe), poi conferma.
5. Il sistema invia all'owner una email con: nome di chi chiede, applicazione richiesta, nota, e un
   collegamento diretto alla scheda dell'applicazione nel catalogo. L'email è nelle cinque lingue e usa il
   renderer condiviso.
6. Il collaboratore vede l'esito: «richiesta inviata al titolare dell'account», e da quel momento la scheda
   mostra «**già richiesto il …**» invece del comando.

## 5. Flussi alternativi / edge / errori

- **Limite di frequenza**: una richiesta per applicazione ogni **ventiquattro ore** per persona. Oltre, il
  comando è disabilitato con la spiegazione e la data dell'ultima richiesta. Serve a non trasformare una
  funzione utile in una seccatura per l'owner.
- **Edge — richiesta già inviata da un altro collaboratore**: l'owner riceve la seconda richiesta (è
  informazione: due persone la vogliono), ma l'email dice che è la seconda. Nessun accorpamento
  complicato: solo un contatore.
- **Errore — invio dell'email non riuscito**: si dice la verità («non è stato possibile avvisare il
  titolare, riprova più tardi») e **non** si registra la richiesta come inviata. Una richiesta registrata e
  non recapitata è peggio di un errore visibile.
- **Edge — l'owner ha disattivato le comunicazioni**: le comunicazioni di servizio non sono soggette a
  consenso di marketing, ma vanno comunque contate come email transazionali con la loro gestione dei
  rimbalzi.
- **Edge — applicazione non installabile** (per esempio la voce di piattaforma dei posti, UC 0103): non
  compare nel catalogo, quindi il caso non esiste. Va provato che non compaia.
- **Errore — collaboratore che chiama l'interfaccia di acquisto direttamente**: rifiuto nel servizio. Il
  catalogo in sola lettura è comodità; il presidio è nel core.

## 6. Schermate & stati

La pagina del catalogo resta identica nella struttura (ricerca, filtri, schede, paginazione) e cambia solo
nei comandi. Regola applicata: qui si **sostituisce** il comando invece di disabilitarlo, perché al
collaboratore non manca un permesso su quella funzione — quella funzione non è sua. Il rimpiazzo è una
funzione **utile**, che è ciò che rende accettabile la sostituzione.

Sulla scheda di dettaglio: prezzi e livelli restano **visibili** (informazione utile per motivare la
richiesta), il comando di acquisto è sostituito.

Stati: caricamento, pronto, richiesta inviata, già richiesto entro le ventiquattro ore, errore di invio.

## 7. Dati toccati

- **Nuova tabella `platform.app_install_request`**: account, applicazione, chi ha chiesto, nota, istante.
  Serve al limite di frequenza, alla traccia «già richiesto» e a sapere che cosa i collaboratori chiedono —
  informazione di prodotto tutt'altro che inutile.
- **Dati personali**: la **nota** è testo libero scritto da una persona, quindi va trattata come tale:
  *categoria* contenuto fornito dall'utente; *finalità* trasmettere la richiesta al titolare dell'account;
  *base giuridica* esecuzione del contratto; *conservazione* breve, proposta novanta giorni (serve al limite
  di frequenza e alla traccia, non oltre). Il nome di chi chiede è già trattato. Va dichiarato nel manifesto
  dei dati della piattaforma e nel registro dei trattamenti: **è l'unico trattamento nuovo dell'intera
  epica**, e per questo va segnalato a chi cura la conformità.
- L'email all'owner contiene nome dell'applicazione, nome di chi chiede e nota: nessun altro dato.

## 8. Permessi & gate

- **La lettura del catalogo non richiede diritti** (regola già in vigore).
- **L'acquisto resta dell'owner**, con presidio nel core.
- **La richiesta è di chiunque sia autenticato nell'account**, compreso un `viewer`: è una richiesta, non
  un potere.
- **Limite di frequenza applicato nel servizio**, non nell'interfaccia.
- **Nessun dato personale nell'email oltre il minimo** necessario.

## 9. Requisiti di test

- **Componente**: per un collaboratore le schede mostrano il comando di richiesta; per l'owner il comando
  di acquisto (prova di non-regressione).
- **Integrazione**: la richiesta crea la riga, invia l'email, e la seconda richiesta entro
  ventiquattro ore è rifiutata con il messaggio giusto.
- **Integrazione**: invio non riuscito → nessuna riga registrata.
- **Integrazione**: un collaboratore che chiama l'acquisto riceve un rifiuto.
- **Email**: resa nelle cinque lingue, con il renderer condiviso, e verifica che non contenga altro che il
  minimo.
- **Percorso end-to-end di livello 2** su `frontend/apps/backoffice/e2e/catalog.spec.ts` (esistente, da
  estendere); nel percorso di piattaforma la ricezione dell'email è verificabile con la casella di prova
  già in uso.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [UC 0095](../../21-catalogo-app-backoffice/0095-pagina-app-catalog.md),
  [AppCatalogPage.tsx](../../../../frontend/apps/backoffice/src/pages/catalog/AppCatalogPage.tsx),
  [UC 0085](../../17-skill-e-tooling-contenuto/0085-unificazione-renderer-email-commons.md).
- **Definition of Done**:
  1. il catalogo è navigabile dal collaboratore, senza comandi di acquisto;
  2. la richiesta di installazione funziona, con limite di frequenza e traccia;
  3. l'email arriva all'owner nelle cinque lingue, col minimo dei dati;
  4. il trattamento della nota è dichiarato nel manifesto e nel registro dei trattamenti;
  5. `run-tests.sh frontend backend compliance` verde più il percorso aggiornato.

## Punti aperti / decisioni differite

- **Notifica dentro il prodotto** (non solo email) per l'owner: sarebbe la sede naturale, ma un centro
  notifiche non esiste. Annotato in [docs/_BACKLOG.md](../../../_BACKLOG.md).
- **«Chiedi l'abilitazione» a una applicazione già attiva**: introdotto qui come caso gemello; se generasse
  troppo rumore, si valuterà di limitarlo. Proprietario: questa storia.
- **Conservazione della nota**: proposta novanta giorni, da confermare con chi cura la conformità.
  Proprietario: UC 0031 (rilevatore dei segnali privacy) in fase di implementazione.

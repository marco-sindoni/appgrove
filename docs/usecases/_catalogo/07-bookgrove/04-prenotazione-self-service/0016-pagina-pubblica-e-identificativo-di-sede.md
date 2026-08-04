# 0016 — Pagina pubblica e identificativo di sede

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 04 — Prenotazione self-service del cliente finale
**Storia**: `0016` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio un indirizzo mio da mettere sul profilo social, sul biglietto da visita e sulla vetrina
> così che chi vuole un appuntamento se lo prenda da solo, anche alle undici di sera, senza telefonare.

**Contesto.** Questa storia apre la **superficie pubblica** dell'applicazione, ed è il punto in cui BookGrove si
scosta dall'invariante numero uno della piattaforma — «l'identificativo dell'account arriva solo dal token di
accesso verificato». Chi apre questa pagina non ha nessun token: è una persona che non ha alcun rapporto con
appgrove. La deviazione è descritta per esteso al §11, punto 3, della descrizione dell'applicazione ed è
**approvata a parte dallo sviluppatore**, non decisa qui.

Questa storia consegna la pagina in **sola lettura**: si vede l'attività, i suoi servizi pubblici e gli orari
liberi. L'atto di prenotare arriva con la storia `0017`, e le difese con la `0019`.

## 2. Requisiti funzionali

1. **RF-1** — L'attività sceglie il proprio **identificativo pubblico** (per esempio `salone-da-lucia`),
   modificabile finché è libero; l'indirizzo risultante è stabile e condivisibile.
2. **RF-2** — La pagina mostra il nome dell'attività, i servizi marcati come pubblici con durata e prezzo
   indicativo, e gli spazi liberi in una finestra limitata (per esempio le prossime quattro settimane).
3. **RF-3** — La pagina **non mostra nulla di più**: nessun nome di cliente, nessuna prenotazione esistente,
   nessun dato che non sia stato pubblicato di proposito. Uno spazio occupato appare solo come non disponibile.
4. **RF-4** — L'attività sceglie se la pagina è **pubblicata** o **sospesa**; da sospesa risponde con una pagina
   neutra che invita a contattare l'attività.
5. **RF-5** — La pagina è nella lingua di chi la apre fra `en, it, fr, es, de`, con ricaduta sulla lingua
   predefinita; l'attività può indicare la propria lingua principale.
6. **RF-6** — La pagina porta un'**informativa breve** e visibile senza cercarla: chi tratta i dati (l'attività,
   con appgrove come fornitore), quali dati raccoglie e perché.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1) — deviazione dichiarata.** Qui **non c'è un token di accesso**. Il
  `tenant_id` si ricava dall'**identificativo pubblico di sede** presente nel percorso, risolto **dal server**
  contro la tabella delle pagine pubbliche: non arriva mai dal corpo della richiesta né da un'intestazione, e non
  è un dato che il chiamante possa scegliere per sé. L'identificativo **non è un segreto e non è una credenziale**:
  è un nome pubblicato dall'attività, per questo le difese non possono essere la segretezza (storia `0019`). Ciò
  che concede è il minimo: servizi pubblici, spazi liberi, e — dalla storia `0017` — il diritto di *proporre* una
  prenotazione. Nessuna lettura di prenotazioni esistenti, nessun dato personale di nessuno.
  **Questa è la deviazione dall'invariante ed è approvata a parte** (§11, punto 3, della descrizione).
- **RT-2 — Interfaccia di programmazione (§2).** Rotte pubbliche separate e riconoscibili, per esempio
  `GET /api/prenotazioni/v1/pubblico/{identificativo}` e
  `GET /api/prenotazioni/v1/pubblico/{identificativo}/disponibilita`, servite da un percorso distinto da quello
  autenticato; risposte in `problem+json`; OpenAPI aggiornata. Un identificativo inesistente e uno sospeso
  rispondono **allo stesso modo**, per non rivelare quali attività esistono.
- **RT-3 — Persistenza (§8).** Migrazione `V9__pagina_pubblica.sql`: tabella `pagina_pubblica` con `tenant_id`,
  UUID versione 7, colonne di controllo, cancellazione logica, e un vincolo di unicità **globale**
  sull'identificativo pubblico — è l'unico dato dell'app che deve essere unico fra tutti gli account, e va detto
  perché è un'eccezione notevole.
- **RT-4 — Frontend (§3, §5).** La pagina pubblica **non vive dentro il backoffice**: è una superficie a sé, che
  usa gli stessi token del sistema di design, funziona in tema chiaro e scuro, si legge da telefono e non carica
  nulla che richieda un'autenticazione.
- **RT-5 — Cinque lingue (§4).** Tutti i testi della pagina in `en, it, fr, es, de`; i nomi dei servizi restano
  nella lingua in cui li ha scritti l'attività.
- **RT-6 — Dati personali (§10).** Nessun dato personale **raccolto** in questa storia (la raccolta arriva con la
  `0017`). L'informativa breve è comunque obbligatoria da subito. **Nessun tracciamento**: niente strumenti di
  analisi, niente cookie non tecnici, nessun banner di consenso.
- **RT-7 — Registrazione eventi (§14).** `pagina pubblica aperta`, `identificativo sconosciuto` con `tenant_id`
  quando noto, `app_id` e correlazione; l'indirizzo di rete resta nel dato applicativo della storia `0019`, non
  nel registro tecnico.
- **RT-8 — Prove (§11).** Prova di sicurezza dedicata: identificativo inesistente, sospeso, con caratteri
  inattesi, e tentativo di leggere una prenotazione esistente attraverso le rotte pubbliche — tutti respinti
  allo stesso modo.

## 4. Criteri di accettazione

**CA-1 — La pagina si apre e mostra il giusto**
- **Dato** un'attività con quattro servizi, due dei quali solo interni
- **Quando** una persona apre l'indirizzo pubblico
- **Allora** vede i due servizi pubblici con durata e prezzo, e gli spazi liberi delle prossime settimane

**CA-2 — Niente trapela**
- **Dato** un'agenda con appuntamenti presi · **Quando** si guarda la pagina pubblica e la risposta che la
  alimenta · **Allora** non compaiono né nomi di clienti né servizi prenotati: solo spazi non disponibili

**CA-3 — Identificativo sconosciuto e pagina sospesa**
- **Dato** un identificativo inesistente e uno sospeso · **Quando** li si apre · **Allora** si vede la **stessa**
  pagina neutra in entrambi i casi, e nulla lascia capire se l'attività esista

**CA-4 — Unicità dell'identificativo**
- **Dato** un identificativo già usato da un'altra attività · **Quando** un secondo account prova a prenderlo
- **Allora** l'operazione è rifiutata con un messaggio chiaro

**CA-5 — Lingua e informativa**
- **Dato** un visitatore con lingua del navigatore in francese · **Quando** apre la pagina · **Allora** la vede in
  francese, con l'informativa breve visibile senza doverla cercare

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sulla risoluzione dell'identificativo e di **integrazione** sulle rotte pubbliche;
- [ ] prova di **isolamento fra account** in forma di **prova di sicurezza** sulle rotte pubbliche (RT-8);
- [ ] **prova end-to-end**: **coperta ora** — è il primo passo del percorso del cliente finale
      `[J-BOOKGROVE-PUB]`, creato dalla storia `0034`, dove si aggiorna
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** della pagina pubblica in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova in questa storia, con la ragione scritta;
- [ ] **registro delle decisioni** compilato: **la deviazione sull'origine del `tenant_id`, la differenza fra
      identificativo pubblico e gettone di capacità, e il perché** — è la decisione più importante
      dell'applicazione;
- [ ] avvio locale: la pagina pubblica è raggiungibile dal proxy locale senza cablaggi a mano;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0010` | la pagina mostra gli spazi liberi calcolati dal motore |
| **approvazione dello sviluppatore** sull'accesso senza autenticazione | è una deviazione da un invariante di piattaforma |

## 7. Fuori ambito

- prenotare: storia `0017`;
- limiti di frequenza, verifica del contatto e difese: storia `0019`;
- la personalizzazione grafica avanzata della pagina (logo, colori dell'attività): rimandata, non richiesta dalla
  scheda di catalogo; per ora la pagina porta il nome dell'attività e i token del sistema di design.

## 8. Punti aperti

**Indicizzazione dai motori di ricerca.** Alcune attività vogliono essere trovate, altre no. La proposta è che
l'impostazione predefinita sia **non indicizzabile**, con un interruttore per chi vuole il contrario: è la scelta
prudente, perché una pagina di prenotazione indicizzata è anche una pagina che i sistemi automatici scoprono.
Da confermare dallo sviluppatore, ed è una decisione di prodotto oltre che tecnica.

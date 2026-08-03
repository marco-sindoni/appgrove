# 0021 — Ricezione dei documenti passivi

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 04 — Trasmissione e ciclo di vita legale
**Storia**: `0021` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`, `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile amministrativo
> voglio ricevere in un solo posto le fatture che i miei fornitori mi mandano per via elettronica
> così da non doverle raccogliere da una casella di posta certificata, da un portale e da un allegato che qualcuno
> mi inoltra.

**Contesto.** La ricezione è metà dell'obbligo e viene sistematicamente dimenticata nelle analisi, perché è meno
visibile dell'emissione. Eppure in Germania la **ricezione** è obbligatoria dal gennaio 2025 mentre l'emissione
arriva solo nel 2027, e in Francia dal 1° settembre 2026 tutti devono essere in grado di **ricevere** anche se non
devono ancora emettere (descrizione dell'applicazione §2.3). È anche la voce «emissione e ricezione fattura
elettronica» della scheda di catalogo.

Va detto subito il conto: sulla rete a quattro angoli il fornitore fattura **anche la ricezione**, a €0,18-0,25 a
documento. La ricezione non è gratuita per noi, e la decisione se farla pesare sulla quota del cliente è di
listino, non tecnica.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio acquisisce i documenti in ingresso dai canali configurati e li riconduce al modello
   canonico, marcandoli come **passivi**.
2. **RF-2** — L'acquisizione è idempotente per identificativo del documento e mittente: lo stesso documento non
   entra due volte.
3. **RF-3** — Il documento passivo viene associato al **soggetto emittente** dell'account a cui è indirizzato; se
   non si riesce ad associarlo, finisce nella coda dei non elaborati con il motivo.
4. **RF-4** — La controparte mittente viene riconciliata con l'anagrafica; se non esiste, viene creata come
   fornitore in bozza.
5. **RF-5** — L'elenco dei documenti distingue chiaramente attivi e passivi, con un filtro dedicato; i documenti
   passivi **non** si trasmettono e non hanno il pulsante «Trasmetti».
6. **RF-6** — Il documento passivo entra nel percorso di conservazione a norma come quello attivo (epica 05):
   l'obbligo decennale vale per entrambi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il `tenant_id` del documento passivo si ricava dal soggetto emittente
  destinatario, **mai** da un campo del carico in ingresso. È il punto in cui un documento potrebbe finire
  nell'account sbagliato: prova di isolamento dedicata e obbligatoria.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta di sola lettura
  `GET /api/einvoicing/v1/documents?direzione=passivi`; l'ingresso avviene dal punto d'ascolto autenticato del
  fornitore, non da una rotta pubblica. Errori in `application/problem+json`; definizione OpenAPI aggiornata nello
  stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V17__inbound_documents.sql`: colonna `direzione` su
  `canonical_document`, indice su `(tenant_id, direzione, data)`, e tabella di deduplica dei documenti in
  ingresso. `tenant_id`, chiave UUID versione 7, colonne di controllo.
- **RT-4 — Modulo frontend (§3, §5).** Filtro «attivi / passivi» nell'elenco dei documenti e scheda di dettaglio
  in sola lettura, senza le azioni di trasmissione. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Le etichette «emesse / ricevute» e i messaggi di associazione mancata dallo
  spazio-nomi `einvoicing`, presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** ⚠️ La ricezione **consuma una unità** della metrica `documenti`, perché ha
  un costo verso il fornitore. Due conseguenze non negoziabili: l'interfaccia deve dirlo chiaramente, e **a quota
  esaurita il documento in ingresso non si rifiuta**. Rifiutare un documento fiscale in ingresso per una questione
  di abbonamento sarebbe un danno al cliente: si accetta, si registra lo sforamento, e si avvisa. È l'unica deroga
  al blocco per quota di tutta l'app e va motivata nel registro delle decisioni.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: `list_documents` della storia `0011`
  copre anche i passivi con il filtro di direzione.
- **RT-8 — Dati personali (§10).** **Sì, e con una novità**: qui i dati personali riguardano i **fornitori** del
  cliente, non i suoi clienti. Le voci del manifesto vanno estese al soggetto interessato «fornitore», in italiano
  e inglese, e la tabella di deduplica dei documenti in ingresso — che conserva il carico — va in `exportData` e
  `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `documento passivo acquisito`, `documento passivo non
  associabile`, `ricezione oltre quota` sono registrati con `tenant_id`, `app_id`, `user_id` (di sistema),
  identificativo di correlazione, senza carichi né denominazioni.

## 4. Criteri di accettazione

**CA-1 — Ricezione riuscita**
- **Dato** un account con un soggetto emittente belga configurato sulla rete
- **Quando** arriva un documento in ingresso a lui indirizzato
- **Allora** esiste un documento canonico marcato passivo, con mittente riconciliato in anagrafica

**CA-2 — Documento non associabile**
- **Dato** un documento in ingresso il cui destinatario non corrisponde a nessun soggetto emittente dell'account
- **Quando** viene acquisito
- **Allora** finisce nella coda dei non elaborati con il motivo, e nulla viene perso

**CA-3 — Doppia consegna**
- **Dato** lo stesso documento consegnato due volte dal canale
- **Quando** entrambe le consegne sono acquisite
- **Allora** esiste un solo documento passivo

**CA-4 — I passivi non si trasmettono**
- **Dato** un documento passivo
- **Quando** si apre la scheda
- **Allora** non esiste alcuna azione di trasmissione, e un tentativo sulla rotta di trasmissione è rifiutato

**CA-5 — Ricezione oltre quota**
- **Dato** un account che ha esaurito la quota del mese
- **Quando** arriva un documento in ingresso
- **Allora** il documento **viene comunque acquisito**, lo sforamento è registrato e l'utente è avvisato in
  panoramica

**CA-6 — Isolamento fra account**
- **Dato** un documento in ingresso il cui carico contiene l'identificativo di un altro account
- **Quando** viene acquisito
- **Allora** finisce nell'account del soggetto emittente destinatario, e l'identificativo del carico è ignorato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sull'associazione al soggetto destinatario e sulla deduplica; **integrazione**
      sull'acquisizione dal fornitore simulato;
- [ ] prova di **isolamento fra account** con documento in ingresso che tenta di attraversare gli account;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-EINVOICING]` (storia `0030`) includerà l'arrivo di un
      documento passivo e la sua comparsa nell'elenco filtrato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con il soggetto interessato «fornitore» e con la tabella di deduplica in
      esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la **deroga al blocco per quota in ricezione** e il suo motivo;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo, e il motivo è scritto.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0011` | Serve il modello canonico in cui ricondurre i documenti in ingresso |
| `0019` | Il punto d'ascolto delle notifiche e la coda dei non elaborati sono gli stessi |
| `0017`, `0018` | I canali configurati dal lato emissione sono gli stessi da cui si riceve |

## 7. Fuori ambito

- L'**approvazione** del documento passivo, il suo ciclo di autorizzazione e il pagamento: è un altro dominio
  (ProcureGrove 48 nel catalogo, e l'incasso in CashGrove 03). Qui il documento si riceve, si conserva e si
  espone.
- La lettura di formati **non** elettronici (un documento inviato per posta ordinaria o come immagine): fuori
  ambito, è estrazione da documenti (ExtractGrove 61 nel catalogo).
- La ricezione italiana dai canali di posta certificata: rimandata; nella prima versione la ricezione italiana
  passa dal fornitore configurato, come l'emissione.

## 8. Punti aperti

- 🛑 **Se la ricezione debba consumare la quota del cliente.** Tecnicamente ha un costo per noi; commercialmente è
  controintuitivo far pagare per ricevere, e nessun concorrente italiano lo fa. È una decisione di listino, quindi
  una fermata di escalation dello sviluppatore. La proposta qui — consumare ma non bloccare — è un compromesso, non
  una decisione presa.
- **Per quanto tempo si conserva un documento passivo di cui il cliente non è l'emittente.** L'obbligo di
  conservazione decennale vale anche per i passivi, ma il cliente potrebbe non volerli tutti. Non si può decidere
  qui: è conformità, e va portata al presidio trasversale insieme al punto della storia `0026`.

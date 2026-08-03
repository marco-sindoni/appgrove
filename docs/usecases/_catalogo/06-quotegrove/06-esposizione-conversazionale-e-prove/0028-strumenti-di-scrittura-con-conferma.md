# 0028 — Strumenti di scrittura con bozza e conferma

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 06 — Esposizione conversazionale e prove
**Storia**: `0028` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0027`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che detta un preventivo mentre guida
> voglio poter dire «fai un preventivo a Fornitura Alfa per dieci ore di installazione» e ritrovarmelo pronto da
> controllare
> così da arrivare in ufficio con il lavoro fatto a metà, ma senza che nulla sia partito senza il mio consenso.

**Contesto.** È la storia che rende concreta la promessa del catalogo — comandare l'azienda da una chat — e
insieme il punto in cui si applica la regola di sicurezza non negoziabile: **l'intelligenza artificiale prepara,
la persona approva**. Gli strumenti che producono effetti verso l'esterno — mandare un documento a un cliente,
sollecitarlo — non possono mai eseguire da soli.

## 2. Requisiti funzionali

1. **RF-1** — Sono dichiarati cinque strumenti di scrittura, ciascuno con nome stabile, descrizione, schemi,
   marcatura *scrittura* e indicazione della conferma richiesta:
   `crea_preventivo(destinatario, righe, validita?, modello?)`,
   `aggiorna_righe_preventivo(id, righe)`,
   `invia_preventivo(id, destinatario_invio)` — **irreversibile, verso l'esterno**,
   `sollecita_preventivo(id, testo?)` — **irreversibile, verso l'esterno**,
   `registra_esito(id, esito, motivo?)`.
2. **RF-2** — Ogni strumento di scrittura produce **una bozza dell'effetto** — cosa cambierebbe, su quale
   documento, con quali numeri — e non esegue finché non arriva una conferma umana esplicita.
3. **RF-3** — Per `invia_preventivo` e `sollecita_preventivo` la conferma è **obbligatoria e non aggirabile**:
   nessuna configurazione, nessuna modalità, nessun consenso preventivo può renderle automatiche.
4. **RF-4** — La bozza scade: una conferma che arriva molto dopo, su dati nel frattempo cambiati, viene rifiutata
   e va rifatta.
5. **RF-5** — **Nessuno strumento accetta o rifiuta un preventivo al posto del destinatario**: il divieto è
   dichiarato nel contratto, non solo omesso.
6. **RF-6** — `invia_preventivo` consuma la metrica `preventivi_inviati` esattamente come l'invio
   dall'interfaccia: a quota esaurita la conferma è rifiutata con `429`.

## 3. Requisiti tecnici

- **RT-1 — Esposizione conversazionale (§12).** Strumenti dichiarati con firma, descrizione, schemi e marcatura
  scrittura; quelli con effetti irreversibili producono una bozza e richiedono conferma umana. Il contratto vive
  dentro il servizio; il server è di piattaforma — **dipendenza dichiarata: casi d'uso 0061-0064**.
- **RT-2 — Isolamento fra account (§1).** Il contesto dell'account arriva dal livello di piattaforma; nessuno
  schema di strumento contiene un parametro di account; ogni scrittura filtra per `tenant_id`.
- **RT-3 — Varchi e quota (§6, §7).** Le chiamate dell'assistente passano dagli stessi cinque varchi e consumano
  la stessa quota: **un assistente non è una scorciatoia per superare un limite**. Con abbonamento non attivo la
  risposta è `402`.
- **RT-4 — Ruoli (§6).** Uno strumento non può fare ciò che l'utente per conto del quale agisce non potrebbe fare:
  se il suo ruolo non basta per approvare uno sconto, nemmeno l'assistente lo approva.
- **RT-5 — Dati personali (§10).** Nessun campo di persona nuovo, ma le bozze contengono dati del destinatario e
  vivono per un tempo limitato: vanno dichiarate nel manifesto con la loro durata.
- **RT-6 — Registrazione eventi (§14).** `bozza proposta`, `bozza confermata`, `bozza scaduta`, `esecuzione
  rifiutata` con `tenant_id`, `app_id`, `user_id`, correlazione e nome dello strumento — **è la traccia che
  permette di ricostruire cosa ha proposto la macchina e cosa ha approvato la persona**.
- **RT-7 — Prove (§11).** Prova che nessuna via di invocazione permette di eseguire `invia_preventivo` senza
  conferma; prova che la quota è consumata come dall'interfaccia; prova di contratto sugli schemi.

## 4. Criteri di accettazione

**CA-1 — La bozza si vede prima**
- **Dato** l'invocazione di `crea_preventivo` con destinatario e righe · **Quando** lo strumento risponde
- **Allora** restituisce la bozza con righe, prezzi risolti dal listino e totale, e **nessun preventivo esiste
  ancora**

**CA-2 — Conferma e creazione**
- **Dato** la bozza precedente · **Quando** la persona conferma · **Allora** il preventivo esiste in stato
  `bozza`, con l'indicazione che è stato proposto dall'assistente e confermato da quella persona

**CA-3 — Invio senza conferma: impossibile**
- **Dato** l'invocazione di `invia_preventivo` · **Quando** si tenta di eseguirla senza conferma, per qualunque
  via · **Allora** l'operazione è rifiutata e **nessun messaggio parte**

**CA-4 — Quota esaurita anche dalla chat**
- **Dato** un account con `preventivi_inviati` esaurita · **Quando** la persona conferma un invio proposto
  dall'assistente · **Allora** riceve `429` con il rimedio e nulla parte

**CA-5 — Bozza scaduta**
- **Dato** una bozza proposta e poi un cambio del listino · **Quando** la conferma arriva dopo la scadenza della
  bozza · **Allora** l'esecuzione è rifiutata e va rifatta con i dati aggiornati

**CA-6 — Nessuna accettazione per procura**
- **Dato** il contratto degli strumenti · **Quando** lo si esamina · **Allora** non esiste alcuno strumento che
  accetti o rifiuti un preventivo al posto del destinatario, e la prova che lo verifica è verde

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sul ciclo bozza-conferma e di **integrazione** su ciascuno strumento;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sugli strumenti di scrittura;
- [ ] **prova end-to-end**: nessun impatto sulla superficie utente finché il livello conversazionale non esiste —
      risposta scritta nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni**: non applicabile;
- [ ] **manifesto dei dati** aggiornato con le bozze e la loro durata;
- [ ] **registro delle decisioni** compilato: **la regola bozza + conferma, il divieto di accettazione per procura
      e la durata delle bozze**;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0027` | condivide il contratto e il modo di dichiararlo |
| casi d'uso di piattaforma 0061-0064 (non implementati) | server conversazionale, consenso delegato, applicazione di abilitazione e quota alle chiamate dell'assistente |

## 7. Fuori ambito

- la generazione automatica dei testi commerciali: non è di questa storia;
- l'esecuzione automatica di qualunque cosa esca dall'azienda: esclusa per principio, non per ora.

## 8. Punti aperti

Nessuno: la regola di sicurezza è del catalogo e non è negoziabile a livello di app.

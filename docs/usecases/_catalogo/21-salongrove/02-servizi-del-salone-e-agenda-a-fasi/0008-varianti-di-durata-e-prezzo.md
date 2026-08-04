# 0008 — Varianti di durata e prezzo

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 02 — Servizi del salone e agenda a fasi
**Storia**: `0008` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come operatrice che conosce le sue clienti
> voglio poter dire che per Anna il colore dura venti minuti in più e costa quindici euro in più, perché ha i
> capelli lunghi
> così da non fare ogni volta lo stesso calcolo a mente e da non far aspettare la cliente delle 16.

**Contesto.** Nel beauty il prezzo di listino è un punto di partenza: lunghezza, quantità di ricrescita, doppia
applicazione e prodotti di linea alta cambiano sia il tempo sia il conto. Le fonti di settore riportano un prezzo
del colore fra 55 e 120 € (§2.5 della descrizione): l'ampiezza di quella forbice **è** questa storia. Senza
varianti il salone finisce per creare quindici servizi diversi che sono lo stesso servizio, e il catalogo diventa
illeggibile.

## 2. Requisiti funzionali

1. **RF-1** — Un servizio può avere **varianti**: nome, minuti in più (anche negativi), importo in più, e a quale
   fase si applicano i minuti aggiuntivi.
2. **RF-2** — Al momento della prenotazione si scelgono zero o più varianti, e durata e prezzo indicativo
   dell'appuntamento si aggiornano di conseguenza **prima** di confermare.
3. **RF-3** — Le varianti scelte abitualmente da un cliente vengono **proposte** alla prenotazione successiva,
   come suggerimento modificabile, non come automatismo.
4. **RF-4** — Le varianti scelte restano attaccate alla prenotazione e finiscono nel conto: la riga del conto
   riporta «Colore + capelli lunghi», non un importo che nessuno sa spiegare.
5. **RF-5** — Le varianti sono facoltative: un salone che non ne definisce nessuna non se ne accorge.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Varianti e loro scelta filtrano per `tenant_id` dal token verificato,
  **compreso il suggerimento**: la variante abituale di un cliente non deve poter essere dedotta da un altro
  account.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST|PUT|DELETE /api/<app>/v1/servizi/{id}/varianti` e
  scelta delle varianti nella creazione della prenotazione; corpo validato (durata risultante positiva); errori in
  `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Tabella `variante_servizio` e tabella di legame con la prenotazione, con
  `tenant_id`, UUID versione 7, colonne di controllo e cancellazione logica. Gli importi in **centesimi interi**.
- **RT-4 — Coerenza con i segmenti (storia `0007`).** I minuti aggiuntivi si applicano alla fase dichiarata e
  **rigenerano i segmenti**: una variante non può produrre un'occupazione incoerente con quella già scritta.
- **RT-5 — Modulo frontend (§3, §5).** Nella prenotazione, le varianti sono caselle da spuntare con l'effetto
  mostrato accanto («+20 minuti, +15 €») e il totale che si aggiorna sotto gli occhi. Solo token del sistema di
  design.
- **RT-6 — Cinque lingue (§4).** Etichette ed errori in `en, it, fr, es, de`; i **nomi delle varianti** li scrive
  il salone e restano nella sua lingua.
- **RT-7 — Dati personali (§10).** La **preferenza abituale** del cliente è un dato che riguarda una persona: va
  dichiarata nel manifesto in italiano e inglese, con finalità «proporre la scelta abituale», base «esecuzione del
  contratto» e durata allineata a quella della scheda tecnica. Campo annotato, tabella in esportazione e
  cancellazione.
- **RT-8 — Registrazione eventi (§14).** `variante applicata alla prenotazione` con `tenant_id`, `app_id`,
  `user_id` e correlazione, senza il nome del cliente.

## 4. Criteri di accettazione

**CA-1 — La variante allunga e rincara**
- **Dato** un colore da 80 minuti a 60 € e una variante «capelli lunghi» (+20′ sulla fase di applicazione, +15 €)
- **Quando** si prenota il colore con quella variante
- **Allora** l'appuntamento dura 100 minuti, il prezzo indicativo è 75 €, e la fase di applicazione è di 40 minuti

**CA-2 — I segmenti si rigenerano**
- **Dato** l'appuntamento del caso precedente
- **Quando** si guarda l'occupazione della poltrona
- **Allora** copre 100 minuti, non 80, e nessun segmento è rimasto alla durata vecchia

**CA-3 — Il suggerimento è un suggerimento**
- **Dato** una cliente che negli ultimi tre appuntamenti ha sempre scelto «capelli lunghi»
- **Quando** si prenota il quarto
- **Allora** la variante è già spuntata ma si può togliere, e toglierla non produce nessun avviso

**CA-4 — Variante impossibile rifiutata**
- **Dato** un servizio da 30 minuti
- **Quando** si crea una variante da −40 minuti
- **Allora** l'errore è chiaro e nulla viene salvato

**CA-5 — Isolamento fra account**
- **Dato** due account con una cliente omonima
- **Quando** un utente del primo prenota
- **Allora** i suggerimenti nascono solo dallo storico del proprio account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`);
- [ ] prove di **unità** sul ricalcolo di durata, prezzo e segmenti, di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account** su varianti e suggerimenti;
- [ ] **prova end-to-end**: *rimando* — passo del percorso `[J-SALONGROVE]` della storia `0030`;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per la preferenza abituale del cliente, con campo
      annotato e tabella in esportazione e cancellazione;
- [ ] **registro delle decisioni**: suggerimento e non automatismo, varianti applicate a una fase specifica,
      importi in centesimi;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | le varianti agiscono su una fase |
| storia `0007` | i segmenti vanno rigenerati coerentemente |

## 7. Fuori ambito

- gli sconti e le promozioni sul conto: storia `0019`;
- i listini per fascia oraria o per giorno («il martedì il taglio costa meno»): non in questa stesura, e sarebbe
  una storia a sé;
- il prezzo definitivo: quello lo fa il conto, qui è un prezzo **indicativo**.

## 8. Punti aperti

**Il suggerimento è una piccola profilazione.** Proporre a una cliente la variante che sceglie sempre è comodo e
innocuo, ma è comunque una decisione presa a partire dal suo storico. La proposta è che resti sempre visibile,
sempre modificabile e mai automatica — cioè che sia un promemoria per l'operatore, non una scelta per la cliente.
Se lo sviluppatore volesse spingersi oltre (per esempio proporre servizi), quella sarebbe una funzione diversa con
una valutazione diversa.

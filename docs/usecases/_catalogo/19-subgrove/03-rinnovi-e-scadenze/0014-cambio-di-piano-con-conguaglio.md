# 0014 — Cambio di piano con conguaglio

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 03 — Rinnovi e scadenze
**Storia**: `0014` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetta alla reception a cui un iscritto chiede di passare dal piano base a quello completo a metà mese
> voglio che l'app mi dica quanto deve pagare in più adesso e da quando cambia il canone
> così da poterglielo dire subito, con un numero giusto, invece di rispondere «ti faccio sapere».

**Contesto.** Il cambio di piano a metà periodo è la domanda che manda in crisi i fogli di calcolo, perché
richiede un conto proporzionale che nessuno ha voglia di fare a mano. La regola che questa storia adotta è
**quella già decisa dalla piattaforma per sé** ([docs/09-pagamenti.md](../../../../09-pagamenti.md) dec. 22, e
§10.1 della descrizione): passaggio a un piano superiore **subito**, con conguaglio proporzionale per i giorni
che restano; passaggio a un piano inferiore **a fine periodo**, senza rimborso del periodo in corso. Non è
pigrizia: è una semantica già ragionata, coerente con lo standard del settore, che si può spiegare all'abbonato
in due frasi — ed è esattamente il genere di riuso che il §10.1 raccomanda.

## 2. Requisiti funzionali

1. **RF-1** — Si può cambiare il piano di un abbonamento vivo scegliendo il piano di destinazione; l'app
   riconosce da sola se è un passaggio verso l'alto o verso il basso, confrontando gli importi normalizzati a
   mese.
2. **RF-2** — Passaggio verso l'alto: ha effetto **subito**; l'app genera una scadenza di **conguaglio**
   proporzionale ai giorni restanti del periodo, e dal periodo successivo si applica il canone nuovo.
3. **RF-3** — Passaggio verso il basso: è **programmato** a fine periodo; fino a quel giorno restano piano,
   canone e condizioni attuali, e l'app lo dice a chiare lettere.
4. **RF-4** — Prima di confermare, l'app mostra un riepilogo che dice **cosa** succede e **quando**: importo del
   conguaglio, data di decorrenza del canone nuovo, e per il passaggio verso il basso la data esatta da cui
   varrà.
5. **RF-5** — Un cambio programmato si può **annullare** finché non ha avuto effetto, e la scheda
   dell'abbonamento lo mostra sempre.
6. **RF-6** — Il conguaglio si calcola **a giorni** sul periodo in corso, con la regola scritta a schermo perché
   l'addetta possa spiegarla.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Abbonamento e piano di destinazione devono appartenere allo stesso
  account del token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `POST /api/abbonati/v1/abbonamenti/{id}/cambio-piano/anteprima` e `.../cambio-piano`, più
  `DELETE .../cambio-piano` per annullare il programmato; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V9__cambio_programmato.sql`: i campi del cambio programmato sulla
  tabella `abbonamento` (piano di destinazione, data di efficacia) e la scadenza di conguaglio come normale riga
  di `scadenza` con causale distinta — **non** una tabella nuova.
- **RT-4 — Modulo frontend (§3, §5).** Finestra di cambio piano con anteprima e conferma esplicita; sulla scheda,
  l'avviso permanente «dal giorno X passerai al piano Y»; solo token del sistema di design, tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Riepilogo, regola del conguaglio e avvisi in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6).** Il cambio di piano **non** cambia il conteggio della quota di appgrove:
  l'abbonamento resta uno. Con abbonamento di piattaforma non attivo, `402`.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `cambia_piano(abbonamento, nuovo_piano, quando) → bozza con conguaglio calcolato`, marcato **scrittura**:
  produce una bozza e richiede conferma umana, perché cambia quanto una terza persona pagherà.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: si tocca il denaro, non l'anagrafica.
- **RT-9 — Registrazione eventi (§14).** `cambio piano applicato`, `cambio piano programmato`, `cambio annullato`,
  con importi e date, `tenant_id`, `app_id`, `user_id` e correlazione.
- **RT-10 — Prove (§11).** Unità sul calcolo del conguaglio, compresi i casi limite: cambio il primo giorno del
  periodo, l'ultimo giorno, e su un periodo annuale.

## 4. Criteri di accettazione

**CA-1 — Passaggio verso l'alto con conguaglio**
- **Dato** un abbonamento mensile a 30 €, periodo dall'1 al 30, e un cambio al piano da 60 € eseguito il giorno 16
- **Quando** l'utente conferma
- **Allora** nasce una scadenza di conguaglio di 15 € (quindici giorni alla differenza di 30 € al mese), il piano
  è già quello nuovo, e dal periodo successivo il canone è 60 €

**CA-2 — Passaggio verso il basso programmato**
- **Dato** lo stesso abbonamento e un cambio a un piano da 20 €
- **Quando** l'utente conferma
- **Allora** nulla cambia adesso, la scheda dice «dal 1° del mese prossimo passerai al piano da 20 €», e non
  nasce alcuna scadenza di conguaglio né alcun rimborso

**CA-3 — Annullamento del programmato**
- **Dato** un cambio verso il basso programmato · **Quando** l'utente lo annulla prima della data
- **Allora** l'abbonamento resta sul piano attuale e l'avviso sparisce

**CA-4 — Anteprima onesta**
- **Dato** un cambio in preparazione · **Quando** si apre l'anteprima
- **Allora** l'importo mostrato è **esattamente** quello che verrà creato: nessuna differenza fra anteprima ed
  esecuzione

**CA-5 — Isolamento fra account**
- **Dato** un abbonamento dell'account `A` e un piano dell'account `B` · **Quando** si prova il cambio
- **Allora** la richiesta è rifiutata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sul conguaglio proporzionale, casi limite compresi; **integrazione** sulle rotte;
- [ ] prova di **isolamento fra account** sul cambio;
- [ ] **prova end-to-end**: *rimando* — il cambio di piano non entra nel percorso principale; voce `da-coprire`
      nel registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con storia
      proprietaria `0033`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato: **riuso dichiarato** della semantica di piattaforma (su verso
      l'alto subito con conguaglio, verso il basso a fine periodo senza rimborso) e regola di calcolo a giorni;
- [ ] contratto dello strumento `cambia_piano` dichiarato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | servono periodi e scadenze su cui calcolare |
| storia `0007` | il piano di destinazione ha una versione di prezzo viva a cui agganciarsi |

## 7. Fuori ambito

- il cambio **chiesto dall'abbonato** dal portale: storia `0025`, dove è una richiesta da approvare, non un
  comando;
- lo spostamento in blocco di molti abbonamenti su una versione di prezzo nuova: storia `0007`;
- i rimborsi: non esistono in questa app, perché non esistono incassi (§5.2 della descrizione).

## 8. Punti aperti

**Se il conguaglio non viene incassato.** Un conguaglio è una scadenza come le altre e finisce nella catena dei
solleciti (storia `0021`); ma l'abbonato **sta già usando** il piano superiore. Sospendere per un conguaglio non
pagato è sproporzionato, tenerlo aperto in eterno è ingenuo. **Proposta**: il conguaglio non provoca sospensione
automatica, ma se non rientra entro il periodo si riporta l'abbonamento al piano precedente dalla scadenza
successiva, avvisando. Non l'ho messo fra i requisiti perché è una decisione commerciale. Chiude: lo sviluppatore.

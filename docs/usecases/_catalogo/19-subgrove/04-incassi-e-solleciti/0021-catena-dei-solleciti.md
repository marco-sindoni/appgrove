# 0021 — Catena dei solleciti

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 04 — Incassi e solleciti
**Storia**: `0021` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ogni mese perde qualche centinaio di euro per addebiti tornati indietro
> voglio che chi non ha pagato riceva un promemoria subito e poi ancora, senza che io ci pensi
> così da recuperare la maggior parte di quel denaro senza fare io la parte dell'esattore.

**Contesto.** È **il prodotto**, non un accessorio. L'analisi in rete (§2.5 della descrizione) dice tre cose
convergenti: l'abbandono **involontario** — l'addebito che fallisce, non il cliente che se ne va — pesa fra il
20% e il 40% del totale con punte più alte; una catena di solleciti ben fatta ne recupera fra il 50% e l'80%; e
la parte del leone si gioca nelle **prime 72 ore**, con un decadimento netto dopo due settimane. Da qui il
disegno: il primo sollecito parte **subito**, non «a fine mese»; i successivi si diradano; la catena ha una fine,
perché insistere all'infinito non recupera nulla e diventa molestia.

Le fonti sono di fornitori, con l'interesse che ne consegue: le ho lette come ordini di grandezza, non come
misure. Ma la direzione che indicano è coerente e vale come criterio di disegno.

## 2. Requisiti funzionali

1. **RF-1** — Quando una scadenza risulta fallita o resta non incassata oltre la data di esigibilità, si apre una
   **catena di solleciti** per quella scadenza.
2. **RF-2** — La catena è configurabile per account, come sequenza di passi (giorno 0, giorno 3, giorno 7, giorno
   14…) con un tetto massimo di passi; i valori predefiniti seguono l'evidenza del §2.5 e sono modificabili.
3. **RF-3** — Ogni sollecito lascia una **prova**: quando, a quale recapito, quale messaggio, con quale esito di
   consegna. La scheda dell'abbonamento mostra la catena e a che punto è.
4. **RF-4** — La catena si **ferma da sola** quando la scadenza rientra, quando l'abbonamento cessa, o quando i
   passi sono esauriti; l'esito finale è registrato.
5. **RF-5** — Il cliente può **escludere** un singolo abbonato dai solleciti automatici, con un motivo: ci sono
   sempre casi che si trattano a voce, e senza questa valvola il cliente spegne tutto.
6. **RF-6** — Esiste un tetto di sicurezza **non aggirabile** al numero di messaggi per abbonato in una finestra
   di tempo, indipendente dalla configurazione: nessuna configurazione deve poter trasformare i solleciti in
   molestia.
7. **RF-7** — Se sull'account è attiva **03 CashGrove**, alla fine della catena la scadenza non incassata le
   viene **consegnata** e SubGrove smette di sollecitare: due catene sullo stesso denaro non devono coesistere.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Catene, prove e configurazione filtrate per `tenant_id` dal token
  verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/abbonati/v1/solleciti` (con filtri),
  `POST /api/abbonati/v1/scadenze/{id}/sollecita` (invio a mano),
  `PUT /api/abbonati/v1/impostazioni/solleciti`; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V16__sollecito.sql`: tabella `sollecito` con `tenant_id`, colonne di
  controllo, progressivo, canale, momento, esito, recapito usato; l'esclusione dell'abbonato è un campo sulla
  sua anagrafica.
- **RT-4 — Comunicazioni.** Il messaggio si compone con il **renderer condiviso** della piattaforma (change
  `0079`); il canale del primo giro è la posta elettronica. La messaggistica breve introdurrebbe un fornitore
  esterno e resta fuori.
- **RT-5 — Comunicazione fra app (§2).** La consegna a CashGrove avviene **a evento**, mai con una chiamata
  diretta; se CashGrove non c'è, l'evento resta senza consumatori e la catena si chiude comunque.
- **RT-6 — Modulo frontend (§3, §5).** Nella panoramica, «da recuperare» con importo e numero di scadenze; nella
  scheda, la catena con i suoi passi; nelle impostazioni, la configurazione dei passi; solo token del sistema di
  design.
- **RT-7 — Cinque lingue (§4).** Interfaccia in `en, it, fr, es, de`; il **messaggio** va nella lingua
  dell'abbonato, con lo stesso avvertimento della storia `0013`.
- **RT-8 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `sollecita_scadenza(scadenza, canale) → bozza del messaggio`, marcato **scrittura irreversibile** — manda un
  messaggio a una persona che non è il nostro utente — con **conferma umana obbligatoria**. Nessuno strumento
  manda solleciti in blocco.
- **RT-9 — Dati personali (§10).** Il sollecito conserva il recapito usato e la prova: voce nuova nel manifesto
  in italiano e inglese, campo annotato, tabella in `exportData` e `purgeData`.
- **RT-10 — Registrazione eventi (§14).** `sollecito inviato (progressivo)`, `catena chiusa (esito)`,
  `abbonato escluso dai solleciti`, `tetto di sicurezza raggiunto`, con `tenant_id`, `app_id`, `user_id` e
  correlazione, **senza** recapito né contenuto.

## 4. Criteri di accettazione

**CA-1 — Primo sollecito immediato**
- **Dato** una scadenza registrata come fallita
- **Quando** gira la lavorazione dello stesso giorno
- **Allora** parte il primo sollecito e la catena mostra «passo 1 di 4, prossimo fra 3 giorni»

**CA-2 — La catena si ferma da sola**
- **Dato** una catena al passo 2 · **Quando** la scadenza viene registrata incassata
- **Allora** non parte alcun sollecito successivo e la catena si chiude con esito «rientrata»

**CA-3 — Tetto di sicurezza**
- **Dato** una configurazione con passi molto ravvicinati e un abbonato con tre scadenze scoperte
- **Quando** girano le lavorazioni
- **Allora** il numero di messaggi verso quell'abbonato non supera il tetto di sicurezza, e l'evento è registrato

**CA-4 — Esclusione**
- **Dato** un abbonato escluso dai solleciti · **Quando** una sua scadenza fallisce
- **Allora** non parte alcun messaggio, ma la scadenza resta negli scoperti e il motivo dell'esclusione è visibile

**CA-5 — Consegna a CashGrove**
- **Dato** un account con CashGrove attiva e una catena esaurita senza rientro
- **Quando** la catena si chiude
- **Allora** viene pubblicato l'evento di consegna e SubGrove non manda più solleciti per quella scadenza

**CA-6 — Isolamento fra account**
- **Dato** due account · **Quando** uno legge i solleciti · **Allora** vede solo i propri

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`);
- [ ] prove di **unità** sulla progressione della catena e sul tetto di sicurezza; **integrazione** sulla
      lavorazione;
- [ ] prova di **isolamento fra account**;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-ABBONATI]` fa fallire una scadenza e verifica che il
      primo sollecito parta; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** dell'interfaccia in cinque lingue e del messaggio nelle lingue supportate per gli abbonati;
- [ ] **manifesto dei dati** aggiornato con `sollecito`, recapito compreso;
- [ ] **registro delle decisioni** compilato: prima catena tarata sull'evidenza del §2.5, tetto di sicurezza non
      aggirabile, confine con CashGrove;
- [ ] contratto dello strumento `sollecita_scadenza` dichiarato con conferma obbligatoria;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0018` | la catena si apre da un fallimento registrato |
| storia `0013` | riusa l'impianto di invio e di prova degli avvisi |
| **03 CashGrove** (app del catalogo, non implementata) | consumatore della consegna; finché non c'è, la catena si chiude e basta |

## 7. Fuori ambito

- la sospensione dell'abbonamento a catena esaurita: storia `0022`;
- il canale di messaggistica breve: introdurrebbe un fornitore esterno, fuori dal nucleo;
- il recupero legale del credito: non è di questa app e nemmeno della suite.

## 8. Punti aperti

**Il tono dei messaggi.** Un sollecito troppo morbido non funziona, uno troppo duro fa perdere il cliente; e chi
lo firma non è appgrove ma il **nostro cliente**, che ha una sua voce. **Proposta**: testo predefinito neutro,
personalizzabile per account, con le parti obbligatorie (importo, periodo, come pagare, come disdire) non
rimovibili. Chiude: lo sviluppatore, con la direzione di prodotto.

**Quante volte si può scrivere a una persona che non è nostro utente.** Il tetto di sicurezza c'è, ma il valore
giusto non lo so: dipende anche da regole sulle comunicazioni commerciali che non ho verificato, e un sollecito
di pagamento non è una comunicazione commerciale, ma il confine va guardato. Chiude: revisione legale.

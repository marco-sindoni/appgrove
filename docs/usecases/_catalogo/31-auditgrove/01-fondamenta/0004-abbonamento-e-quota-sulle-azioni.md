# 0004 — Abbonamento e quota sulle azioni

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un account che paga AuditGrove
> voglio sapere quante azioni ho registrato rispetto al tetto del mio piano, essere avvisato prima di arrivarci, e
> sapere esattamente cosa succede quando ci arrivo
> così da non scoprire il problema nel momento peggiore, cioè quando mi serve una prova che non è stata scritta.

**Contesto.** La metrica di quota di AuditGrove è `actions`, di natura `flow`: ogni riga accodata nella catena
consuma una unità. Ma questa app ha un problema che nessun'altra del catalogo ha: **bloccare significa perdere
prove**. La regola di piattaforma è chiara — al raggiungimento del tetto si blocca con `429`, non si addebita a
sorpresa ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §7) — e resta valida; ma applicarla senza
accorgimenti produrrebbe esattamente ciò che il prodotto promette di non essere: un registro con un buco
silenzioso. La risposta proposta al §5 della [descrizione dell'applicazione](../application-description.md) è
duplice: una **banda di cortesia** oltre il tetto, e — quando anche quella finisce — un rifiuto che **conta** le
azioni respinte, così il buco è dimostrabile e misurato invece che invisibile.

## 2. Requisiti funzionali

1. **RF-1** — Ogni riga accodata nella catena consuma una unità della metrica `actions`; il consumo è contato
   sulla finestra mensile che si azzera (natura `flow`).
2. **RF-2** — L'account riceve un avviso al raggiungimento dell'**80 %** e del **100 %** del tetto del proprio
   piano, con l'indicazione di quanto manca alla fine della finestra e di come rimediare.
3. **RF-3** — Superato il 100 % l'ingresso **continua ad accettare** azioni per una **banda di cortesia**
   dichiarata; le azioni accettate in banda sono marcate come tali e restano prove a tutti gli effetti.
4. **RF-4** — Esaurita anche la banda di cortesia, l'ingresso risponde `429` con un messaggio che dice come
   rimediare, e **nessuna azione viene scritta**.
5. **RF-5** — Ogni rifiuto per quota incrementa un contatore, e a intervalli regolari (e comunque alla chiusura
   della finestra) il servizio accoda nella catena una **riga di rifiuto**: «da … a …, `N` azioni respinte per
   quota esaurita». Il buco resta un buco, ma è **dimostrabile e misurato**.
6. **RF-6** — L'accesso all'app segue la catena dei varchi comune: con abbonamento in `trialing`, `active` o
   `past_due` la funzione resta accessibile; con `canceled` o `paused` risponde `402`.
7. **RF-7** — I diritti dell'interessato — esportazione e cancellazione dei dati personali — restano accessibili
   **in ogni caso**, anche ad abbonamento scaduto, a quota esaurita e ad app disabilitata.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il conteggio della quota è per account, ricavato dal `tenant_id` del
  token verificato o della credenziale di sorgente verificata (storia 0006); un `tenant_id` che arrivasse dal
  corpo della richiesta viene ignorato. Il consumo di un account non è mai visibile a un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/agentaudit/v1/quota` che restituisce consumo,
  tetto, stato della banda e momento di azzeramento; errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit. Il `429` porta l'indicazione del rimedio nel corpo del problema.
- **RT-3 — Persistenza (§8).** Migrazione `V3__quota_e_rifiuti.sql` sullo schema `app_agentaudit`: tabella del
  consumo per finestra e tabella dei rifiuti contati, entrambe con `tenant_id`, chiave primaria UUID versione 7 e
  colonne di controllo. La riga di rifiuto accodata nella catena segue le regole della storia 0002: sola aggiunta,
  `deleted_at` mai valorizzato.
- **RT-4 — Modulo frontend (§3, §5).** Nella shell del modulo `agentaudit` compare l'indicatore di consumo della
  metrica; a quota esaurita compare l'avviso bloccante. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I testi degli avvisi (80 %, 100 %, banda di cortesia, quota esaurita) passano
  dallo spazio-nomi `agentaudit` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Prima di accodare un'azione il servizio prenota una unità della metrica
  `actions` (natura `flow`); a quota **e** banda esaurite risponde `429` con l'indicazione del rimedio. Con
  abbonamento non attivo risponde `402`. L'abilitazione si legge dalla **proiezione locale** alimentata a eventi,
  mai con una chiamata sincrona all'app centrale sul percorso caldo.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo. Il consumo di quota delle chiamate
  dell'assistente è materia della storia 0036, che si aggancia a questo conteggio.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: il conteggio è un numero per account, e la riga di
  rifiuto contiene un intervallo e una quantità, non identità.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «soglia dell'80 % superata», «tetto raggiunto», «ingresso in
  banda di cortesia», «azione respinta per quota» sono registrati con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Il consumo si conta**
- **Dato** un account sul piano `pro` con tetto di 50.000 azioni al mese e nessun consumo
- **Quando** vengono accodate 120 azioni
- **Allora** la rotta della quota riporta 120 azioni consumate, il tetto del piano e la data di azzeramento

**CA-2 — L'avviso arriva prima del guaio**
- **Dato** un account che ha consumato il 79 % del proprio tetto
- **Quando** l'accodamento porta il consumo oltre l'80 %
- **Allora** l'account riceve l'avviso dell'80 % una sola volta, con l'indicazione del rimedio, e le azioni
  continuano a essere accettate

**CA-3 — La banda di cortesia accetta e marca**
- **Dato** un account che ha appena raggiunto il 100 % del tetto
- **Quando** dichiara una nuova azione
- **Allora** l'azione **viene scritta** nella catena, è marcata come accettata in banda di cortesia, e l'account
  riceve l'avviso del tetto raggiunto

**CA-4 — Il rifiuto è contato, non silenzioso**
- **Dato** un account che ha esaurito tetto e banda di cortesia
- **Quando** dichiara tre nuove azioni
- **Allora** riceve tre risposte `429` con l'indicazione del rimedio, nessuna azione viene scritta, e nella catena
  compare (subito o alla chiusura della finestra) una riga che dichiara l'intervallo e il numero di azioni
  respinte

**CA-5 — L'abbonamento comanda l'accesso**
- **Dato** un account con abbonamento in `past_due` e uno con abbonamento `canceled`
- **Quando** entrambi tentano di dichiarare un'azione
- **Allora** il primo viene servito normalmente e il secondo riceve `402`; **entrambi** possono comunque chiedere
  l'esportazione dei propri dati personali

**CA-6 — I conteggi non si mescolano fra account**
- **Dato** due account `A` e `B`, con `A` a quota esaurita e `B` a metà del proprio tetto
- **Quando** `B` dichiara un'azione
- **Allora** l'azione di `B` viene accettata, e nessuna interrogazione di `A` mostra il consumo di `B`, nemmeno
  forzando l'identificativo dell'altro account nella richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo del consumo, delle soglie e della banda, e di **integrazione** sulla rotta
      della quota e sul rifiuto, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sui conteggi e sui rifiuti;
- [ ] **prova end-to-end**: risposta «rimando» — l'avviso di quota e il blocco entrano nel percorso
      `[J-AGENTAUDIT]` alla storia 0037, proprietaria della copertura; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta l'esenzione motivata fino ad
      allora;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`) per i quattro avvisi;
- [ ] **manifesto dei dati**: nessuna voce nuova, e il fatto è dichiarato;
- [ ] **registro delle decisioni** compilato, con **due voci obbligatorie**: l'esistenza della banda di cortesia e
      il motivo per cui esiste (bloccare un registro di prova non è come bloccare un'altra app), e la scelta di
      accodare una riga che conta i rifiuti;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia, dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la convenzione di quota di piattaforma viene applicata in modo particolare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | Il listino dichiara la metrica `actions`, il tetto per piano e la natura `flow` |
| storia `0002` | Il consumo si aggancia all'accodamento nella catena, e la riga di rifiuto è essa stessa una riga incatenata |
| Proiezione locale dell'abilitazione, alimentata a eventi | L'abilitazione non si chiede all'app centrale sul percorso caldo |

## 7. Fuori ambito

- la scelta dei prezzi e dei tetti: è una fermata di escalation dello sviluppatore (§5 della descrizione
  dell'applicazione), la storia li consuma e non li fissa;
- il comportamento della **conservazione** al passaggio a un piano inferiore: storia 0016, dove il problema è
  diverso e più grave (non si perdono prove future, si distruggono prove già acquisite);
- il consumo di quota da parte delle chiamate dell'assistente: storia 0036.

## 8. Punti aperti

- ⚠️ **Ampiezza e forma della banda di cortesia — fermata di escalation dello sviluppatore.** Quanto larga (una
  percentuale del tetto? un numero fisso? un numero di giorni?), se sia una sola volta per finestra o ripetibile,
  e se il piano gratuito ne abbia diritto. Sono decisioni di prodotto e di prezzo: la storia le assume come
  parametro configurabile, **non le decide**.
- ⚠️ **Il comportamento al tetto è esso stesso una scelta commerciale.** Accettare oltre il tetto per non perdere
  prove è generoso e costoso; rifiutare è coerente con la piattaforma ma produce buchi. La proposta qui è la via
  di mezzo; conferma lo sviluppatore.
- **Ogni quanto accodare la riga che conta i rifiuti.** Una riga per rifiuto consumerebbe quota a sua volta (che
  è assurdo); una riga alla chiusura della finestra è economica ma tardiva. Propongo una riga ogni intervallo
  fisso e comunque una alla chiusura, senza consumo di quota. Da confermare.

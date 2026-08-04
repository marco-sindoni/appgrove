# 0023 — Definizione di un budget

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 05 — Budget, avvisi e anomalie
**Storia**: `0023` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha deciso di non spendere più di 800 € al mese in modelli
> voglio scrivere quel numero dentro l'app e dire chi va avvisato
> così da smettere di controllare a mano ogni due giorni se sto sforando.

**Contesto.** Il budget è la funzione che, secondo la scheda di catalogo, separa chi prova dall'app da chi la
compra: il segnale di traction è la quota di prove che ne imposta uno. Va quindi resa facilissima e va posta
**presto** nel percorso del cliente. Una precisazione da fare fin dall'inizio, in questa storia e nell'interfaccia:
il budget di TokenGrove è un **tetto sorvegliato**, non un rubinetto. Non siamo in mezzo alle chiamate e non
possiamo fermarle (§3.1 del documento capofila): quello che possiamo fare è accorgercene presto e dirlo bene, e
mettere a disposizione il semaforo della storia `0027`.

## 2. Requisiti funzionali

1. **RF-1** — Un budget si definisce con: ambito (tutto l'account, oppure un valore di una dimensione, per esempio
   «cliente = acme»), importo, periodo (mese solare o intervallo fisso), soglie di avviso e destinatari.
2. **RF-2** — Le soglie predefinite sono tre — 50%, 80% e 100% — e si possono cambiare. Ogni soglia dice cosa
   scatta e a chi.
3. **RF-3** — Il testo dell'interfaccia dichiara **con chiarezza** che il budget avvisa e non blocca, e rimanda al
   semaforo consultabile per chi vuole fermarsi da solo. La promessa sbagliata qui è peggiore dell'assenza della
   funzione.
4. **RF-4** — La scheda del budget mostra in ogni momento: consumato, residuo, giorni rimanenti, e la previsione
   di fine periodo (storia `0024`).
5. **RF-5** — Un budget si può sospendere e archiviare; gli avvisi già emessi restano nel registro.
6. **RF-6** — L'app **propone** un primo budget quando ha almeno un mese completo di dati, con l'importo suggerito
   pari alla spesa del mese precedente arrotondata: chi non sa quale numero mettere ha comunque un punto di
   partenza.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `budget` filtra per `tenant_id` preso dal
  gettone verificato; un `tenant_id` dal corpo della richiesta viene ignorato.
- **RT-2 — Persistenza (§8).** Migrazione sullo schema `app_spesa_modelli`: tabella `budget` con `tenant_id`,
  ambito, importo, periodo, soglie, destinatari, stato, chiave primaria UUID versione 7, colonne di controllo e
  cancellazione logica.
- **RT-3 — Interfaccia di programmazione (§2).** Rotte `GET|POST|PATCH|DELETE /api/spesa_modelli/v1/budget`; corpo
  validato (importo positivo, soglie crescenti, ambito esistente); errori in `problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-4 — Varchi e ruoli (§6).** Definire un budget è riservato a `owner` e `admin`; un `member` vede i budget
  che riguardano il proprio ambito.
- **RT-5 — Modulo frontend (§3, §5).** Sezione «Budget»; la frase che spiega «avvisa, non blocca» sta accanto al
  campo dell'importo, non in fondo alla pagina. Solo token del sistema di design; tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Tutte le stringhe presenti in `en, it, fr, es, de`, in particolare la frase che
  distingue avviso da blocco: tradurla male sarebbe una promessa sbagliata in quattro lingue.
- **RT-7 — Esposizione conversazionale (§12).** Strumento `definisci_budget(ambito, importo, periodo, soglie) →
  bozza`, marcato **scrittura con conferma** (storia `0033`); e `stato_budget(budget?) → semaforo, consumato,
  previsione`, marcato lettura.
- **RT-8 — Dati personali (§10).** I destinatari degli avvisi sono utenti dell'account, con il loro indirizzo di
  posta: voce `budget.destinatari` nel manifesto in italiano e inglese, campo annotato, tabella in `exportData` e
  `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Eventi «budget creato, modificato, sospeso» con `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione, senza gli indirizzi dei destinatari.

## 4. Criteri di accettazione

**CA-1 — Budget creato e sorvegliato**
- **Dato** un utente `admin`
- **Quando** definisce un budget di 800 € al mese sull'intero account con soglie al 50, 80 e 100%
- **Allora** la scheda mostra consumato, residuo, giorni rimanenti e previsione, e le tre soglie risultano attive

**CA-2 — La promessa è corretta**
- **Dato** la schermata di creazione del budget in una qualunque delle cinque lingue
- **Quando** la si legge
- **Allora** dice esplicitamente che il budget avvisa e non ferma le chiamate, con il rimando al semaforo

**CA-3 — Validazione**
- **Dato** un budget con soglie 80, 50 e 100 in quest'ordine, o con importo negativo
- **Quando** si tenta di salvarlo
- **Allora** è respinto con un messaggio che dice quale vincolo è violato

**CA-4 — Budget suggerito**
- **Dato** un account con un mese completo di dati e nessun budget
- **Quando** apre la sezione Budget
- **Allora** trova la proposta di un budget con l'importo suggerito e la spiegazione da dove viene

**CA-5 — Ruoli e isolamento fra account**
- **Dato** un `member` di un account e un budget di un altro account
- **Quando** tenta di leggerlo o modificarlo forzandone l'identificativo
- **Allora** riceve `403` o `404` e nulla cambia

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione delle soglie e sul suggerimento dell'importo, e di **integrazione**
      sulle rotte;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sui budget;
- [ ] **prova end-to-end**: **coprire ora** il passo «definisco un budget» del percorso `[J-SPESA-MODELLI]` e
      aggiornare il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con revisione mirata della frase «avvisa, non blocca»;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `budget.destinatari`;
- [ ] **registro delle decisioni** compilato, in particolare sul budget come tetto sorvegliato e non come rubinetto;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `definisci_budget` e `stato_budget`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0019` | L'ambito di un budget si appoggia alle dimensioni e ai loro valori |
| Piano dell'account | I budget sono una funzionalità dei piani a pagamento (§5 del documento capofila) |

## 7. Fuori ambito

- la previsione di fine periodo: è la storia `0024` (qui c'è il posto dove si mostra, non il calcolo);
- il recapito degli avvisi: è la storia `0025`;
- qualunque forma di blocco: è la storia `0027`, e non è un blocco nostro.

## 8. Punti aperti

Nessuno.

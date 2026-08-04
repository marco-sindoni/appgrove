# 0018 — Dimensioni di attribuzione

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 04 — Attribuzione della spesa
**Storia**: `0018` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che vuole sapere dove finiscono i soldi
> voglio dichiarare io gli assi con cui la mia azienda ragiona — squadra, progetto, cliente, funzione, ambiente —
> così da vedere la spesa come la vedo nella mia testa, e non come l'ha immaginata chi ha scritto il software.

**Contesto.** L'attribuzione è la metà del prodotto che il rendiconto del fornitore non può dare: il fornitore sa
solo di chiavi, progetti e spazi di lavoro (§2.6, fonti 1-3). Ma gli assi giusti non li sappiamo noi: un'agenzia
ragiona per cliente, un prodotto ragiona per funzionalità, uno studio ragiona per commessa. Dichiarare le
dimensioni **prima** di raccogliere le etichette è ciò che permette di validare quello che arriva invece di
accumulare testo libero — che è il modo con cui il «non attribuito» diventa la voce più grande.

Va detto che questa storia da sola non produce numeri: produce la struttura che le tre successive riempiono. È
piccola apposta, perché è una decisione di modello che conviene isolare.

## 2. Requisiti funzionali

1. **RF-1** — Un account dichiara le proprie dimensioni di attribuzione. Ne esistono cinque **suggerite** e già
   pronte all'attivazione — squadra, progetto, cliente, funzionalità, ambiente — che si possono togliere,
   rinominare e a cui se ne possono aggiungere altre.
2. **RF-2** — Ogni dimensione dichiara: chiave tecnica stabile, nome visibile, se è **obbligatoria** (una misura
   senza quel valore va nel non attribuito ed è segnalata) e se ha un **elenco chiuso** di valori ammessi.
3. **RF-3** — Una dimensione a elenco chiuso rifiuta i valori non previsti; una a elenco aperto li accetta e li
   raccoglie, mostrando quanti valori distinti sono comparsi — perché un elenco aperto che arriva a trecento valori
   è un asse che non serve a nessuno.
4. **RF-4** — All'attivazione dell'app l'account trova le cinque dimensioni suggerite già attive, con una
   spiegazione di una riga ciascuna: chi comincia non deve configurare nulla per vedere il valore.
5. **RF-5** — Le dimensioni non si possono cancellare se hanno misure attribuite: si **archiviano**, restando
   consultabili sui periodi passati. Cancellare un asse cancellerebbe la storia di come si leggevano i conti.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `dimensione` filtra per `tenant_id` preso
  dal gettone verificato; un `tenant_id` dal corpo della richiesta viene ignorato. Prova di isolamento fra due
  account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST|PATCH /api/spesa_modelli/v1/dimensioni`; la
  chiave tecnica è immutabile dopo la creazione (cambiarla renderebbe orfane le etichette già raccolte) e il
  tentativo di cambiarla riceve un errore che lo spiega; errori in `problem+json`; definizione OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** La tabella `dimensione` esiste dalla storia `0002`; qui si aggiungono
  obbligatorietà, elenco chiuso dei valori ammessi e stato di archiviazione.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Attribuzione», scheda «Assi»; solo token del sistema di design;
  funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I nomi e le spiegazioni delle cinque dimensioni suggerite sono presenti in
  `en, it, fr, es, de`; i nomi scelti dall'account restano nella lingua in cui li ha scritti e non si traducono.
- **RT-6 — Varchi e ruoli (§6).** Dichiarare e modificare le dimensioni è riservato a `owner` e `admin`.
- **RT-7 — Esposizione conversazionale (§12).** Le dimensioni sono il parametro `raggruppamento` dello strumento
  `leggi_spesa` (storia `0032`): il contratto dichiara che i valori ammessi si leggono da qui, così che un
  assistente non inventi assi che non esistono.
- **RT-8 — Dati personali (§10).** La dimensione in sé non contiene dati personali; i suoi **valori** sì (storia
  `0019`). La spiegazione mostrata all'account quando crea una dimensione avverte che gli assi non sono il posto
  per identificare persone fisiche.
- **RT-9 — Registrazione eventi (§14).** Eventi «dimensione creata, modificata, archiviata» con `tenant_id`,
  `app_id`, `user_id` e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Si comincia senza configurare**
- **Dato** un account che attiva l'app per la prima volta
- **Quando** apre la scheda degli assi
- **Allora** trova le cinque dimensioni suggerite già attive, ciascuna con la sua spiegazione

**CA-2 — Elenco chiuso**
- **Dato** una dimensione «ambiente» a elenco chiuso con i valori `produzione` e `prova`
- **Quando** arriva una misura con valore `collaudo`
- **Allora** il valore è rifiutato e la misura risulta non attribuita su quell'asse, con il motivo

**CA-3 — Elenco aperto che degenera**
- **Dato** una dimensione a elenco aperto che ha raccolto 300 valori distinti
- **Quando** si apre la scheda degli assi
- **Allora** l'app segnala che quell'asse ha troppi valori per essere utile, con un suggerimento su come chiuderlo

**CA-4 — La chiave non si cambia**
- **Dato** una dimensione con misure già attribuite
- **Quando** si tenta di cambiarne la chiave tecnica
- **Allora** l'operazione è respinta con la spiegazione, mentre il nome visibile si può cambiare liberamente

**CA-5 — Isolamento fra account**
- **Dato** due account con dimensioni omonime
- **Quando** uno le legge
- **Allora** vede solo le proprie, con i propri valori

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sull'immutabilità della chiave e sulla validazione degli elenchi chiusi, e di
      **integrazione** sulle rotte;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulla risorsa `dimensione`;
- [ ] **prova end-to-end**: **si rimanda** alla storia `0034`; da sola questa storia non produce un percorso
      osservabile, ma è il presupposto del passo di attribuzione;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova (i valori sono trattati nella storia `0019`);
- [ ] **registro delle decisioni** compilato, in particolare sulle cinque dimensioni suggerite e sul perché non si
      cancellano;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0002` | Serve la tabella `dimensione` |
| Storia `0003` | Serve la sezione «Attribuzione» del modulo |

## 7. Fuori ambito

- come i valori arrivano sulla misura: è la storia `0019`;
- come si attribuisce ciò che arriva senza etichette: è la storia `0020`;
- il collegamento fra l'asse «cliente» e l'anagrafica clienti di un'altra app appgrove: **non si fa**, perché
  sarebbe una chiamata fra app. L'asse resta testo.

## 8. Punti aperti

Nessuno.

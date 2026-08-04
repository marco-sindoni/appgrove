# 0030 — Imbuto di conversione

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 06 — Report di conversione
**Storia**: `0030` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0015`, `0016` — è la prima dell'epica
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio sapere quante trattative passano da ogni fase alla successiva e quanto tempo ci mettono
> così da capire dove si inceppa la vendita invece di dare la colpa a caso.

**Contesto.** È la ragione per cui la storia 0015 ha imposto uno storico immutabile: senza le righe di passaggio,
un imbuto si può solo stimare. Questa storia trasforma quello storico nella risposta a tre domande — quante ne
entrano, quante ne escono da ogni fase, quanto restano — che sono il valore che un foglio di calcolo non dà.

## 2. Requisiti funzionali

1. **RF-1** — Il rapporto mostra, per un periodo scelto e una pipeline, quante trattative sono **entrate** in
   ciascuna fase, quante sono **uscite** verso la successiva e il tasso di passaggio fra fasi consecutive.
2. **RF-2** — Per ogni fase mostra il **tempo mediano** di permanenza, non la media: una trattativa dimenticata da
   un anno sposterebbe la media e non la mediana.
3. **RF-3** — Il rapporto mostra il tasso di chiusura complessivo (vinte su chiuse) e il tempo mediano dalla
   creazione alla chiusura.
4. **RF-4** — Il periodo si sceglie fra intervalli predefiniti (mese, trimestre, anno, dodici mesi) e un intervallo
   libero; il rapporto dice sempre quante trattative sta considerando.
5. **RF-5** — Quando i numeri sono troppo pochi per essere significativi (meno di dieci trattative chiuse nel
   periodo) il rapporto lo dice esplicitamente, invece di mostrare percentuali che sembrano precise.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'aggregazione parte da `WHERE tenant_id = :tid` sullo storico dei
  passaggi; nessun confronto con dati di altri account, nemmeno in forma aggregata.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/sales/v1/reports/funnel` con periodo, pipeline e
  responsabile; errori in `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: il rapporto è una interrogazione aggregata sullo storico.
  Se le interrogazioni risultassero pesanti, il rimedio è un indice, non una tabella di riepilogo che si
  disallinea.
- **RT-4 — Modulo frontend (§3, §5).** Sezione Rapporti → Imbuto, con la rappresentazione a fasi decrescenti e la
  tabella dei numeri sotto: il grafico attira, la tabella si legge; solo token del sistema di design; tema chiaro
  e scuro; i colori delle fasi vengono dai token e restano distinguibili anche a chi non distingue i colori.
- **RT-5 — Cinque lingue (§4).** Etichette, periodi, espressioni di durata e avvisi in `en, it, fr, es, de`, con
  percentuali e numeri formattati secondo la lingua.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Il rapporto sull'intero account richiede ruolo
  `owner` o `admin`; un `member` vede il proprio portafoglio.
- **RT-7 — Esposizione conversazionale (§12).** È lo strumento `conversion_report` (storia 0034), in sola
  lettura: «com'è andato il trimestre» è una domanda naturale da fare in chat.
- **RT-8 — Dati personali (§10).** Nessun dato personale: sono aggregati su trattative. Nessuna voce nuova.
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo; si registra la durata dell'interrogazione oltre una
  soglia.

## 4. Criteri di accettazione

**CA-1 — Tassi di passaggio**
- **Dato** 20 trattative entrate in «Qualificato» nel trimestre, di cui 12 passate a «Proposta inviata»
- **Quando** si apre il rapporto sul trimestre
- **Allora** il tasso di passaggio fra le due fasi è 60 % e i due conteggi sono visibili

**CA-2 — Mediana e non media**
- **Dato** cinque trattative rimaste in una fase 2, 3, 3, 4 e 200 giorni
- **Quando** si legge il tempo di permanenza
- **Allora** il rapporto mostra 3 giorni, non 42

**CA-3 — Numeri troppo pochi**
- **Dato** un account con 4 trattative chiuse nel periodo
- **Quando** apre il rapporto
- **Allora** compare l'avviso che i numeri non sono significativi, insieme ai conteggi assoluti

**CA-4 — Un membro vede il proprio**
- **Dato** un utente con ruolo `member`
- **Quando** apre il rapporto
- **Allora** vede i numeri del proprio portafoglio, e la richiesta di quelli di tutto l'account riceve `403`

**CA-5 — Isolamento fra account**
- **Dato** due account con storici simili
- **Quando** un utente di `A` apre il rapporto
- **Allora** i numeri comprendono solo `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo di tassi e mediane, casi limite compresi, e di **integrazione**
      sull'aggregazione con storico realistico ma inventato;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli**;
- [ ] **prova end-to-end**: nessun impatto sul percorso minimo; coperta da prove d'integrazione, con il motivo nel
      registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotate la scelta della mediana e la soglia di significatività;
- [ ] contratto degli **strumenti conversazionali**: `conversion_report` in lettura;
- [ ] controllo automatico di **accessibilità** verde sul rapporto, con verifica che i colori non siano l'unico
      modo di distinguere le fasi;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0015` | Lo storico dei passaggi è la sorgente |
| Storia `0016` | Servono le chiusure per il tasso di conversione |

## 7. Fuori ambito

- il confronto con altri account o con medie di settore: fuori perimetro e problematico sui dati dei clienti;
- le previsioni basate sullo storico: fuori perimetro (vedi storia 0017);
- l'esportazione del rapporto: storia 0033.

## 8. Punti aperti

- **Soglia di significatività a dieci trattative** — è una proposta di buon senso, non un criterio statistico.
  Chiude lo sviluppatore.

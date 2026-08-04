# 0029 — Spiegazione dello scostamento

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 06 — Rapporti, esportazione e previsioni
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0015`, `0016`, `0023`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena visto «fatturato di luglio: −18 % rispetto a giugno»
> voglio sapere **dove** si è formata quella differenza — quali clienti, quali categorie, quali sedi —
> così da capire se è un problema o un mese di ferie, senza aprire cinque schermate e fare le sottrazioni a mano.

**Contesto.** La variazione è il numero che fa alzare il telefono, e la domanda che segue è sempre la stessa:
«perché?». Oggi il titolare risponde a naso. La tentazione è far rispondere il modello linguistico: sarebbe
l'errore esatto contro cui è costruita questa app — un'interpretazione plausibile e non verificabile (§2.5 della
[descrizione](../application-description.md), fonte 4). La scomposizione di una differenza è invece un **calcolo
deterministico**: la differenza fra due periodi si spacca lungo una dimensione, ogni pezzo porta il proprio
contributo, e la somma dei contributi fa la differenza totale. È aritmetica, ed è verificabile. Il modello, se
interviene, **legge ad alta voce** la tabella: non la interpreta.

## 2. Requisiti funzionali

1. **RF-1** — Da qualunque valore che mostri un confronto (storia 0015) si chiede la spiegazione dello
   scostamento: si sceglie la **dimensione** lungo cui scomporre fra quelle ammesse dalla metrica.
2. **RF-2** — Il risultato è una tabella ordinata per **contributo assoluto** decrescente: per ogni valore di
   dimensione il valore nel periodo A, quello nel periodo B, la differenza e la quota della differenza totale.
   La somma dei contributi **è** la differenza totale: se non lo è, la scomposizione è un difetto e la schermata
   deve dirlo, non arrotondare.
3. **RF-3** — La tabella distingue tre casi che il conto medio nasconde: valori **comparsi** (assenti in A,
   presenti in B), **spariti** (il contrario) e **variati**. Un cliente perso e uno nuovo che si compensano
   danno differenza zero e sono la cosa più importante da vedere.
4. **RF-4** — La spiegazione porta la sua **scheda del numero** (storia 0016) per entrambi i periodi: se uno dei
   due è `parziale`, la scomposizione lo dichiara **prima** della tabella, perché confrontare un mese completo con
   uno a metà produce una differenza inventata.
5. **RF-5** — Su richiesta il copilota produce un **riassunto di tre righe** della tabella: dice dove si è formata
   la differenza citando i primi tre contributi, e **non** dice perché. Consuma una unità della metrica
   `questions` (storia 0026); la tabella da sola non consuma nulla.
6. **RF-6** — La spiegazione rispetta la classe di riservatezza: la scomposizione di una metrica economica non è
   accessibile a un `member`, e nessuna dimensione riferita a persone dell'account è ammessa (§2.3, punto 3 della
   descrizione).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Le due interrogazioni di periodo filtrano per `tenant_id` preso dal
  gettone verificato; nessuna scorciatoia di calcolo aggira il filtro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/insights/v1/scostamenti` con metrica, periodo
  A, periodo B e dimensione; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: la scomposizione si calcola sui `fatto` già scritti e
  produce una `traccia_del_calcolo` (storia 0016) che cita **entrambi** i periodi. Il risultato non si conserva.
- **RT-4 — Modulo frontend (§3, §5).** La spiegazione è un pannello richiamabile dal confronto: barre di
  contributo positive e negative, ordinate; solo token del sistema di design; il segno della variazione non è
  affidato al solo colore; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe, compresi i nomi dei tre casi (comparso, sparito, variato),
  esistono in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La tabella **non consuma** quota; il riassunto scritto consuma una unità di
  `questions` (natura `flow`) e a quota esaurita la tabella resta accessibile mentre il riassunto risponde `429`.
- **RT-7 — Esposizione conversazionale (§12).** Strumento `spiega_scostamento(metrica, periodo A, periodo B) →
  differenza e scomposizione ordinata per contributo`, marcato **lettura**. Il contratto vive dentro il servizio;
  il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063). Contratto completo nella
  storia 0031.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: la scomposizione mostra le stesse etichette di
  dimensione già dichiarate. Nessuna dimensione «persona dell'account» è ammessa.
- **RT-14 — Registrazione eventi (§14).** «Scostamento calcolato», «riassunto dello scostamento prodotto» con
  `tenant_id`, `app_id`, `user_id`, metrica e identificativo della traccia; senza valori né etichette.

## 4. Criteri di accettazione

**CA-1 — La somma torna**
- **Dato** «fatturato emesso» a 51.600 € in giugno e 42.300 € in luglio, scomposto per cliente
- **Quando** si apre la spiegazione
- **Allora** la tabella elenca i clienti ordinati per contributo, e la somma delle differenze vale esattamente
  −9.300 €

**CA-2 — Comparsi e spariti si vedono**
- **Dato** un cliente presente solo in giugno per 7.000 € e uno presente solo in luglio per 6.800 €
- **Quando** si guarda la tabella
- **Allora** il primo è marcato `sparito` e il secondo `comparso`, entrambi in cima per contributo assoluto,
  invece di sparire dentro una differenza netta di −200 €

**CA-3 — Periodi non confrontabili**
- **Dato** un periodo B ancora in corso (luglio al giorno 12) confrontato con giugno intero
- **Quando** si chiede la spiegazione
- **Allora** prima della tabella compare l'avvertenza che i due periodi non sono confrontabili, con la proposta
  del confronto a pari giorni

**CA-4 — Il riassunto non interpreta**
- **Dato** una scomposizione con tre contributi principali
- **Quando** si chiede il riassunto scritto
- **Allora** il testo cita i tre contributi con i loro valori e **non** contiene ipotesi sulle cause; il contatore
  delle domande aumenta di uno

**CA-5 — Riservatezza**
- **Dato** un utente `member`
- **Quando** chiede la scomposizione di una metrica economica
- **Allora** riceve `403` con il motivo, e non una tabella parziale

**CA-6 — Isolamento fra account**
- **Dato** due account con la stessa metrica
- **Quando** un utente di `A` chiede lo scostamento forzando l'identificativo di account di `B`
- **Allora** il parametro viene ignorato e la scomposizione riguarda solo `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla scomposizione (somma dei contributi uguale alla differenza; casi comparso,
      sparito, variato; valori negativi) e di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sullo scostamento;
- [ ] **prova end-to-end**: *rimando* alla storia 0034; voce `da-coprire` nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** verificato: nessun dato personale nuovo introdotto;
- [ ] **registro delle decisioni** compilato, con «la tabella non consuma, il riassunto sì» e il divieto di
      interpretazione nel riassunto;
- [ ] contratto degli **strumenti conversazionali**: `spiega_scostamento` dichiarato come lettura (storia 0031);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0015` | il confronto fra due periodi è definito lì |
| storia `0016` | la spiegazione porta la scheda del numero di entrambi i periodi |
| storia `0023` | il riassunto scritto passa dal copilota e dalla sua forma di risposta |

## 7. Fuori ambito

- l'**interpretazione** della causa («è successo perché…»): non la fa nessuno, in questa app, ed è una scelta;
- la scomposizione su più dimensioni contemporaneamente: una dimensione alla volta, perché due dimensioni
  producono una tabella che nessuno legge;
- il confronto con un obiettivo o un budget: è l'app 54 BudgetGrove (§11, punto 8 della descrizione);
- la proiezione del periodo in corso a fine mese: storia 0030.

## 8. Punti aperti

- **Quante righe mostrare?** Con 400 clienti la tabella è illeggibile. Proposta: **prime dieci per contributo
  assoluto più una riga «tutti gli altri»**, con l'elenco completo nell'esportazione. Chiude: **sviluppatore**.
- **Il confronto a pari giorni va proposto o imposto?** Imporlo evita l'errore più comune; proporlo rispetta chi
  sa quello che fa. Raccomandazione: **proposto in evidenza, con un gesto solo per applicarlo**. Chiude:
  **sviluppatore**.

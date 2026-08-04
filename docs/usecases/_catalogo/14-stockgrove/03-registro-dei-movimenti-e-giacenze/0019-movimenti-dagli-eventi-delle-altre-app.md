# 0019 — Movimenti dagli eventi delle altre app

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 03 — Registro dei movimenti e giacenze
**Storia**: `0019` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0015`, `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che emette una fattura o batte uno scontrino
> voglio che il magazzino si muova da solo, senza che nessuno debba ricordarsi di registrare lo scarico
> così da avere una giacenza vera anche nei giorni in cui nessuno ha tempo di aggiornare niente.

**Contesto.** È l'aspettativa numero uno rilevata nell'analisi in rete: Fatture in Cloud genera i movimenti
automaticamente all'emissione del documento, e questo è il metro di paragone del segmento (descrizione
dell'applicazione, §2.4 punto 1 e §2.6 fonte 4). Dentro la suite il fatto «venduto» nasce in ShopGrove (29) e il
documento di trasporto in BillGrove (02); StockGrove li riceve **per eventi asincroni** e li trasforma in scarichi.
Nessuna chiamata sincrona fra le app, mai (principi di piattaforma, §2): un magazzino lento non deve poter rallentare
una cassa.

**La regola che rende questa storia diversa dallo scarico manuale.** Quando l'evento arriva, il fatto **è già
avvenuto**: la merce è uscita dal negozio, il cliente se n'è andato con la scatola. Rifiutare quel movimento perché
la giacenza registrata è insufficiente significherebbe lasciare nel programma un saldo che sappiamo falso, e
soprattutto perdere l'unico indizio del perché i conti non tornano. Quindi qui **la giacenza può andare negativa**,
e il negativo è mostrato come **anomalia da spiegare**, non nascosto e non trattato come errore: un articolo a `−3`
sta dicendo che tre pezzi sono usciti senza essere mai entrati, ed è esattamente l'informazione che serve.

## 2. Requisiti funzionali

1. **RF-1** — StockGrove riceve gli eventi delle altre app della suite in modo **asincrono** e ne trae movimenti:
   una vendita diventa uno scarico, un reso diventa un carico, un documento di trasporto in uscita diventa uno
   scarico. Nessuna chiamata sincrona verso le altre app e nessuna in ingresso sul percorso caldo.
2. **RF-2** — Ogni evento ricevuto è registrato con il proprio identificativo di origine: un evento **già visto non
   produce un secondo movimento**, e il secondo arrivo si chiude con esito «già applicato».
3. **RF-3** — Il movimento generato da un evento porta il riferimento al documento d'origine (numero dello
   scontrino, della fattura, del documento di trasporto) e un motivo coerente, così che nello storico si veda da
   dove viene.
4. **RF-4** — Un movimento generato da evento **può portare la giacenza sotto zero**: non viene mai rifiutato per
   merce insufficiente. La coppia articolo-deposito con giacenza negativa è marcata come anomalia e compare in un
   elenco dedicato, con l'indicazione di quanto manca e da quando.
5. **RF-5** — Un evento che si riferisce a un articolo **non riconosciuto** — codice assente dall'anagrafica, codice
   a barre mai visto — non viene scartato in silenzio: finisce in una **coda di eventi non applicabili**, visibile
   all'utente, dove si può collegare l'articolo giusto e applicare l'evento, oppure archiviarlo con un motivo.
6. **RF-6** — Il deposito di destinazione dell'evento è determinato da una regola per account: il deposito
   predefinito, oppure quello associato al punto vendita o alla sede indicata nell'evento quando l'informazione
   c'è.
7. **RF-7** — Gli eventi in errore tecnico (formato illeggibile, campi mancanti) vengono ritentati con attese
   crescenti e, esaurito il numero di tentativi, restano visibili con il loro codice di errore invece di sparire.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni evento porta l'account a cui appartiene e ogni scrittura filtra per
  `tenant_id`; un evento il cui account non corrisponde a nessun account abilitato viene messo in errore, mai
  applicato a un account diverso. L'unicità dell'identificativo dell'evento è **per account**. Prova di isolamento
  fra due account che ricevono eventi con lo stesso identificativo di origine.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica di ingresso per le altre app: l'ingresso è
  **solo** il canale a eventi (principi di piattaforma, §2). Verso l'utente si espongono
  `GET /api/magazzino/v1/eventi-non-applicabili`, `POST /api/magazzino/v1/eventi-non-applicabili/{id}/applica` e
  `.../archivia`, con errori in `application/problem+json` e definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V12__evento_in_ingresso.sql` sullo schema `app_magazzino`: tabella
  `evento_in_ingresso` (identificativo di origine, app di origine, tipo, contenuto ricevuto, stato fra `ricevuto`,
  `applicato`, `non_applicabile`, `in_errore`, tentativi, ultimo errore, movimento generato) con `tenant_id`,
  chiave primaria UUID versione 7, colonne di controllo e cancellazione logica; vincolo di unicità su
  `(tenant_id, app_origine, identificativo_origine)`. Il movimento generato usa la stessa transazione e lo stesso
  aggiornamento condizionato delle altre storie, **senza** la condizione di non negatività.
- **RT-4 — Modulo frontend (§3, §5).** Nuove viste nel modulo `magazzino`: «Giacenze negative» e «Eventi non
  applicabili», entrambe con l'azione per risolvere; solo token del sistema di design; funziona in tema chiaro e
  scuro. La giacenza negativa è mostrata con l'accento di attenzione e una spiegazione in una riga, non come errore
  di sistema.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — compresa la spiegazione della giacenza negativa, che è
  il testo più delicato di questa storia — passano dallo spazio-nomi `magazzino` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** **I movimenti da evento non consumano quota e non vengono mai respinti con
  `429`**: un fatto già avvenuto non si può rifiutare. Un evento che arriva per un account con abbonamento
  `canceled` non viene applicato ma **conservato**, così che riattivando l'app il magazzino non abbia buchi;
  l'applicazione differita è una decisione da confermare (punti aperti).
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento di scrittura**: gli eventi non si generano da
  una chat. L'elenco delle giacenze negative e quello degli eventi non applicabili sono esposti come **lettura** dai
  contratti della storia `0034`. Il server conversazionale è di piattaforma e non ancora implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo** in questa applicazione: l'evento ricevuto porta
  articolo, quantità, deposito e riferimento al documento, **non** il cliente finale, che StockGrove non ha e non
  vuole (descrizione dell'applicazione, §6). Se l'evento contenesse dati del cliente, questi vengono **scartati in
  ingresso** e non finiscono nella tabella: è un requisito, non una raccomandazione. La tabella
  `evento_in_ingresso` va comunque in `exportData` e `purgeData` perché conserva contenuto ricevuto.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `evento applicato`, `evento già applicato`,
  `evento non applicabile`, `giacenza diventata negativa` sono registrati con `tenant_id`, `app_id`, `user_id`
  quando c'è, identificativo di correlazione e identificativo dell'evento di origine, **senza** il contenuto.

## 4. Criteri di accettazione

**CA-1 — La vendita diventa uno scarico**
- **Dato** un articolo con giacenza `10` nel deposito predefinito
- **Quando** arriva un evento di vendita per 2 pezzi di quell'articolo
- **Allora** esiste un movimento di scarico di `−2` con il riferimento allo scontrino, la giacenza è `8` e l'evento
  risulta `applicato`

**CA-2 — Evento ricevuto due volte**
- **Dato** l'evento della prova precedente, già applicato
- **Quando** lo stesso evento arriva una seconda volta con lo stesso identificativo di origine
- **Allora** non viene creato alcun movimento, la giacenza resta `8` e il secondo arrivo si chiude con esito «già
  applicato»

**CA-3 — Il saldo va sotto zero e non viene rifiutato**
- **Dato** un articolo con giacenza `1`
- **Quando** arriva un evento di vendita per 4 pezzi
- **Allora** il movimento viene registrato, la giacenza diventa `−3`, la coppia articolo-deposito compare
  nell'elenco delle giacenze negative con l'indicazione di quanto manca e da quando, e **nessun errore** viene
  restituito all'app di origine

**CA-4 — Articolo non riconosciuto**
- **Dato** un evento che si riferisce a un codice assente dall'anagrafica
- **Quando** l'evento viene elaborato
- **Allora** nessun movimento viene creato, l'evento compare nell'elenco degli eventi non applicabili con il codice
  ricevuto, e collegandolo a un articolo esistente si può applicarlo generando il movimento

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` che ricevono eventi con lo stesso identificativo di origine dalla stessa app
- **Quando** entrambi vengono elaborati
- **Allora** ciascun account ottiene il proprio movimento e nessuno dei due viene scartato per idempotenza a causa
  dell'altro

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla decisione del deposito e sulla mappatura evento → movimento, e di **integrazione**
      sull'elaborazione degli eventi con database effimero e migrazioni vere;
- [ ] prova che il **doppio arrivo** dello stesso evento non produca due movimenti, anche in concorrenza;
- [ ] prova di **isolamento fra account** su eventi con identificativo di origine identico;
- [ ] **prova end-to-end**: *rimando* — l'ingresso a eventi entra nel percorso `[J-MAGAZZINO]` della storia `0036`
      solo per la parte simulabile in locale; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con revisione del testo sulla giacenza negativa;
- [ ] **manifesto dei dati**: `evento_in_ingresso` aggiunta a esportazione e cancellazione; verificato che nessun
      dato del cliente finale entri nella tabella;
- [ ] **registro delle decisioni** compilato, con la scelta di ammettere il negativo per i fatti già avvenuti e di
      conservare gli eventi non applicabili invece di scartarli;
- [ ] contratto degli **strumenti conversazionali**: nessuno strumento di scrittura, con il motivo scritto;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0013` | Registro, giacenza e idempotenza |
| `0015` | Il movimento generato è uno scarico, con la sola differenza della non negatività |
| `0012` | Il riconoscimento dell'articolo dipende dal confine con il catalogo prodotti condiviso |
| ShopGrove (29) e BillGrove (02), non implementate | Finché non esistono, gli eventi si producono con carichi sintetici in prova e con un generatore locale nei dati di prova (`0005`) |

## 7. Fuori ambito

- I connettori verso i negozi online esterni (i grandi mercati elettronici e le piattaforme di commercio
  elettronico): sono fuori perimetro e sono una riduzione consapevole del catalogo da approvare (descrizione
  dell'applicazione, §11 punto 6). Qui si trattano **solo** eventi interni alla suite.
- L'evento che StockGrove **emette** verso le altre app: storia `0020`.
- La correzione di una giacenza negativa: si fa con la rettifica con motivo, storia `0021`, o contando (storia
  `0022`).

## 8. Punti aperti

- **Eventi ricevuti mentre l'abbonamento è `canceled`.** La proposta è di conservarli e non applicarli, applicandoli
  alla riattivazione. È discutibile — un magazzino che si aggiorna da solo dopo mesi di silenzio può sorprendere —
  ed è una scelta di prodotto che chiude lo sviluppatore.
- **Ordinamento degli eventi.** Se una vendita e il suo reso arrivassero invertiti, il saldo finale sarebbe
  comunque corretto (la somma è commutativa), ma la giacenza potrebbe transitare per un negativo che non c'è mai
  stato. La proposta è di accettarlo e di non costruire un riordinatore; va detto perché è visibile nell'elenco
  delle anomalie.

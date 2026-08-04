# 0016 — Chiusura vinta o persa

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 03 — Pipeline e trattative
**Storia**: `0016` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0015`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come venditore
> voglio chiudere una trattativa dichiarando com'è andata e, se è andata male, perché
> così da poter capire dopo qualche mese cosa ci fa perdere le vendite.

**Contesto.** La chiusura è l'unico atto di LeadGrove con **effetti fuori dall'app**: una trattativa vinta è il
preventivo che sta per nascere nell'app 06 e la fattura che arriverà dall'app 02
([application-description.md](../application-description.md) §10). È anche il momento in cui si raccoglie l'unico
dato che rende utile l'epica 06: il motivo della perdita. Senza quel campo obbligatorio, il rapporto «perché
perdiamo» non esiste.

## 2. Requisiti funzionali

1. **RF-1** — Una trattativa aperta si può chiudere come **vinta** o **persa**; la chiusura porta la trattativa in
   uno stato terminale e la toglie dalla lavagna.
2. **RF-2** — La chiusura come persa richiede un **motivo**, scelto da un elenco breve (prezzo, tempi, funzione
   mancante, concorrente, nessuna risposta, altro) con la possibilità di aggiungere una nota; senza motivo la
   chiusura è rifiutata.
3. **RF-3** — La chiusura come vinta registra il valore finale, che può differire da quello stimato.
4. **RF-4** — Una trattativa chiusa si può **riaprire**; la riapertura scrive una riga di storico e la riporta in
   una fase non terminale scelta dall'utente.
5. **RF-5** — La chiusura come vinta emette l'evento «trattativa vinta» destinato alle altre app della suite,
   contenente identificativi, valore e valuta.
6. **RF-6** — La conferma di chiusura è esplicita e dice cosa succede: che la trattativa esce dalla lavagna e che,
   se vinta, l'informazione viene comunicata alle altre app attive.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La chiusura opera solo su trattative dell'account del token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/sales/v1/deals/{id}/close` e
  `POST /api/sales/v1/deals/{id}/reopen`; corpo validato (motivo obbligatorio quando l'esito è «persa»);
  operazione **idempotente** rispetto a un identificativo di richiesta; errori in `application/problem+json`;
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Colonne di esito, motivo di perdita, valore finale e momento di chiusura sulla
  tabella `deal`, aggiunte con migrazione `V<N>__deal_outcome.sql`. La riga di storico si scrive nella stessa
  transazione (storia 0015).
- **RT-4 — Modulo frontend (§3, §5).** Finestra modale di chiusura con esito, motivo e valore finale; la scheda
  della trattativa chiusa mostra esito e motivo in evidenza; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** L'elenco dei motivi di perdita è **di sistema** e va tradotto in
  `en, it, fr, es, de`; la nota libera resta nella lingua in cui è stata scritta.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Chiudere richiede un posto attivo; con abbonamento
  `canceled` la chiusura risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** `close_deal` è **scrittura con effetto verso l'esterno dell'app**
  e richiede **conferma umana obbligatoria** (storia 0035): la chat produce una bozza che dice esito, valore e
  quali app verranno informate; nulla si chiude senza approvazione. Dipendenza dichiarata: UC 0061-0063.
- **RT-8 — Dati personali (§10).** `deal.loss_reason` è già dichiarato nel manifesto come testo libero: qui si
  valorizza, quindi vanno verificati l'annotazione `@PersonalData` e la presenza in esportazione e cancellazione.
  L'evento emesso verso le altre app contiene **identificativi, valore e valuta**, non nomi.
- **RT-9 — Registrazione eventi (§14).** «Trattativa chiusa» con esito, motivo **codificato** (non la nota libera),
  valore, autore e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Chiusura vinta**
- **Dato** una trattativa aperta da 1.200 €
- **Quando** il venditore la chiude come vinta con valore finale 1.100 €
- **Allora** la trattativa è terminale, esce dalla lavagna, la scheda mostra il valore finale e viene emesso
  l'evento «trattativa vinta»

**CA-2 — Chiusura persa senza motivo**
- **Dato** una trattativa aperta
- **Quando** si tenta di chiuderla come persa senza indicare il motivo
- **Allora** riceve `400` con l'indicazione del campo e la trattativa resta aperta

**CA-3 — Riapertura tracciata**
- **Dato** una trattativa persa
- **Quando** il venditore la riapre scegliendo la fase «In negoziazione»
- **Allora** torna sulla lavagna, l'esito è azzerato e lo storico contiene la riga di riapertura

**CA-4 — Doppio invio**
- **Dato** la stessa richiesta di chiusura inviata due volte con lo stesso identificativo di richiesta
- **Quando** arriva la seconda
- **Allora** l'esito è identico e **non** viene emesso un secondo evento

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` tenta di chiudere una trattativa di `B`
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla macchina a stati della trattativa e di **integrazione** su chiusura, riapertura e
      idempotenza;
- [ ] prova di **isolamento fra account** su chiusura e riapertura;
- [ ] **prova end-to-end**: coprire ora — la chiusura come vinta è il passo finale del percorso `[J-SALES]`
      (storia 0037), compresa la verifica che l'evento sia stato emesso; voce nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per i motivi di perdita;
- [ ] **manifesto dei dati** verificato per `deal.loss_reason`, con la nota che l'evento verso le altre app non
      contiene nomi;
- [ ] **registro delle decisioni** compilato, con annotato il contenuto dell'evento «trattativa vinta»;
- [ ] contratto degli **strumenti conversazionali**: `close_deal` dichiarato come scrittura con conferma
      obbligatoria;
- [ ] controllo automatico di **accessibilità** verde sulla finestra di chiusura;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0013` | Serve la trattativa |
| Storia `0015` | La chiusura e la riapertura scrivono storico |
| Contratto degli eventi condivisi della suite | Non esiste ancora ([application-description.md](../application-description.md) §11.4): nel frattempo l'evento si pubblica sul canale asincrono della piattaforma e nessuna app lo consuma |

## 7. Fuori ambito

- la creazione automatica del preventivo nell'app 06: sarebbe spostare la catena del documento contabile dentro
  un'app di vendita (§10 della descrizione dell'applicazione);
- l'analisi dei motivi di perdita: storia 0030 e seguenti;
- le notifiche alla squadra alla chiusura: non previste in questa proposta.

## 8. Punti aperti

- **Elenco dei motivi di perdita** — sei voci di sistema più «altro». Se il cliente volesse i propri, sarebbe un
  campo personalizzato di tipo elenco (storia 0009), ma allora i rapporti confrontabili fra account si perdono. È
  una decisione di prodotto dello sviluppatore.

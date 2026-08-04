# 0023 — Campagna di invio massivo

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 05 — Contatti, campagne e recupero
**Storia**: `0023` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0009`, `0010`, `0022`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio
> voglio mandare lo stesso avviso a un gruppo di clienti, sapendo prima a quanti arriverà e quanto mi costa
> così da fare in tre minuti quello che oggi mi porta via un pomeriggio, senza sorprese sul conto.

**Contesto.** È la funzione che fa pagare l'abbonamento e insieme la più pericolosa dell'app: manda messaggi
veri a persone vere, costa denaro reale e può far scendere il punteggio di qualità del numero fino alla
sospensione. Per questo la storia è costruita attorno a **due presidi**, non attorno alla funzione: si vede
tutto prima (destinatari, esclusi, costo in quota) e si conferma esplicitamente. Il §2.5 dell'analisi indica il
costo imprevedibile come la lamentela numero uno del segmento: qui è dove si risponde.

## 2. Requisiti funzionali

1. **RF-1** — Una campagna sceglie un segmento e un modello approvato, e ne compila i segnaposto — anche con
   valori presi dal contatto (per esempio il nome).
2. **RF-2** — Prima dell'invio l'app mostra: quanti destinatari, **quanti esclusi e perché** (consenso
   revocato, numero non valido), l'anteprima del messaggio con i valori risolti su un destinatario di esempio,
   e il **costo in quota** (`N messaggi con modello sui 2.000 del tuo piano, ne restano M`).
3. **RF-3** — L'invio parte **solo** dopo una conferma esplicita che ripete il numero di destinatari.
4. **RF-4** — Se i destinatari superano la quota residua, la campagna **non parte affatto**: non si invia a
   metà. L'app dice quanti ne mancano.
5. **RF-5** — La campagna si può **interrompere** mentre è in corso; i messaggi già partiti restano partiti,
   quelli non ancora inviati non partono e la quota corrispondente non viene consumata.
6. **RF-6** — Ogni campagna conserva il segmento valutato al momento dell'invio, il modello usato e i suoi
   valori: quello che è stato mandato deve restare ricostruibile.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Segmento, modello, destinatari e invii filtrano per `tenant_id`
  preso dal token verificato. La lavorazione di invio opera **per account**, mai su una coda comune non
  filtrata.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/chat_commerce/v1/campaigns` (bozza),
  `GET .../campaigns/{id}/preflight` (destinatari, esclusi, costo), `POST .../campaigns/{id}/send` (conferma),
  `POST .../campaigns/{id}/stop`; corpo validato; errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Varchi e quota (§6, §7).** L'invio prenota **in blocco** le unità della metrica `messaggi_template`
  necessarie: se non bastano risponde `429` con quante ne mancano e nulla parte. L'interruzione rilascia le
  unità non usate. Con abbonamento `canceled` risponde `402`.
- **RT-4 — Consenso e modelli (storie `0009`, `0010`).** I contatti senza consenso sono esclusi quando il
  modello è di categoria promozionale; un modello non approvato non è selezionabile. Le esclusioni si mostrano
  **prima**, non si scoprono dopo.
- **RT-5 — Persistenza (§8).** Migrazione `V16__campagne.sql`: tabelle `campaign` e `campaign_delivery` con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica. L'invio è
  **idempotente per destinatario**: una ripetizione della lavorazione non manda due volte allo stesso contatto.
- **RT-6 — Ruoli (§6).** Solo `owner` e `admin` possono confermare l'invio di una campagna: è denaro e
  reputazione del numero. Un `member` può preparare la bozza e riceve `403` alla conferma.
- **RT-7 — Dati personali (§10).** Voce nuova nel manifesto in italiano e inglese per
  `campaign_delivery.contact_ref` (a chi è stato inviato che cosa, con l'esito): è la prova di ciò che il
  negozio ha mandato. Tabella aggiunta a `exportData` e `purgeData`.
- **RT-8 — Registrazione eventi (§14).** `campagna confermata` con il numero di destinatari, `campagna
  interrotta`, `campagna conclusa` con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione,
  senza elenchi di persone.
- **RT-9 — Esposizione conversazionale (§12).** `avvia_campagna` è **scrittura irreversibile**: produce una
  bozza con destinatari, esclusi e costo, e richiede conferma umana esplicita. Un assistente non manda
  duemila messaggi da solo.

## 4. Criteri di accettazione

**CA-1 — Si vede tutto prima**
- **Dato** un segmento di 120 contatti, 8 dei quali senza consenso
- **Quando** si prepara una campagna con un modello promozionale
- **Allora** l'app dice «112 destinatari, 8 esclusi per consenso mancante» e «112 messaggi con modello: ne
  restano 1.888 sul tuo piano», e **nulla è partito**
- **Quando poi** il titolare conferma ripetendo il numero di destinatari
- **Allora** l'invio parte e la campagna passa a `in_invio`

**CA-2 — Quota insufficiente**
- **Dato** una campagna con 300 destinatari e una quota residua di 200
- **Quando** si tenta la conferma · **Allora** riceve `429` con «te ne mancano 100», la campagna resta in
  bozza e **nessun messaggio parte**

**CA-3 — Interruzione**
- **Dato** una campagna in corso con 40 messaggi già inviati su 112
- **Quando** si interrompe · **Allora** i restanti 72 non partono, la quota corrispondente torna disponibile e
  la campagna risulta `interrotta`

**CA-4 — Nessun doppione**
- **Dato** una campagna la cui lavorazione viene rieseguita per un guasto
- **Quando** riprende · **Allora** nessun contatto riceve il messaggio due volte

**CA-5 — Ruolo insufficiente**
- **Dato** un utente `member` · **Quando** tenta di confermare l'invio · **Allora** riceve `403` e nulla parte

**CA-6 — Isolamento fra account**
- **Dato** due account · **Quando** `A` invia una campagna · **Allora** nessun contatto di `B` la riceve,
  nemmeno se ha lo stesso numero di telefono di un contatto di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo di destinatari, esclusi e costo, e di **integrazione** sull'invio con il
      canale simulato, compresi quota insufficiente, interruzione e ripetizione della lavorazione;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulle campagne;
- [ ] **prova end-to-end**: *coprire ora* — la campagna con la sua conferma è un passo del percorso
      `[J-CHAT-COMMERCE]`; se il percorso non esiste ancora, voce `da-coprire` con storia proprietaria `0029`
      nel registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue, compresi i motivi di esclusione;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, tabella degli esiti in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, con la prenotazione in blocco della quota e il divieto di invio
      parziale;
- [ ] contratto degli **strumenti conversazionali** dichiarato: `avvia_campagna`, scrittura irreversibile con
      conferma obbligatoria;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0009` | Serve un modello approvato per scrivere fuori dalla finestra |
| `0010` | Il consenso decide chi è escluso |
| `0022` | I destinatari vengono da un segmento |

## 7. Fuori ambito

- la programmazione della campagna a una data futura: utile, è una storia a sé;
- le sequenze di più messaggi (primo invio, sollecito dopo tre giorni): sarebbero automazioni, e le
  automazioni non sono in questa versione;
- il costo **in denaro** della campagna: vedi i punti aperti.

## 8. Punti aperti

- **Costo in denaro invece che in quota.** Mostrare «questa campagna ti costerà 6,80 €» sarebbe la risposta
  perfetta alla lamentela del segmento, ma richiede il listino delle tariffe del fornitore per paese del
  destinatario, che cambia spesso e che oggi non abbiamo in forma affidabile (§2.7 della descrizione). La
  proposta è mostrare il costo **in quota**, che è esatto e verificabile. Se si vorrà il costo in denaro,
  serve prima una fonte ufficiale aggiornata delle tariffe: è una decisione dello sviluppatore.
- **Tetto giornaliero di invii.** Il fornitore applica limiti d'invio per numero che dipendono dalla qualità e
  dallo storico: non conosciamo il valore per un numero nuovo. Servirebbe una prova sul campo.

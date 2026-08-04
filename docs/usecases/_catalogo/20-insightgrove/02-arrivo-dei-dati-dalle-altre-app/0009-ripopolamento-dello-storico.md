# 0009 — Ripopolamento dello storico

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 02 — Arrivo dei dati dalle altre app
**Storia**: `0009` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena collegato la fatturazione
> voglio vedere anche i mesi passati, non solo quello che succederà da adesso in poi
> così da poter confrontare questo mese con lo stesso mese dell'anno scorso il giorno stesso in cui attivo l'app,
> invece che fra tredici mesi.

**Contesto.** Senza ripopolamento, InsightGrove è inutile per un anno: un cruscotto che ha solo il presente non
può confrontare niente, e il confronto è metà del valore (§2.5 della [descrizione](../application-description.md)).
Il ripopolamento non può però essere una lettura del passato altrui: sarebbe la scorciatoia che l'intera
architettura evita. La via è la stessa degli eventi — l'app sorgente **ripubblica** il proprio passato su
richiesta — e la richiesta viaggia anch'essa come evento.

## 2. Requisiti funzionali

1. **RF-1** — Al collegamento di una fonte, InsightGrove pubblica una **richiesta di ripopolamento** per quel
   solo account e quella sola fonte, con la finestra temporale desiderata (proposta: trentasei mesi).
2. **RF-2** — La fonte risponde ripubblicando i propri fatti storici come fatti normali, con le stesse chiavi di
   idempotenza che userebbe in tempo reale: **il ripopolamento non ha un percorso proprio**, usa quello di 0007.
3. **RF-3** — La fonte segnala la **fine** del ripopolamento con un fatto di chiusura che dichiara quanti fatti
   ha pubblicato e su quale finestra.
4. **RF-4** — Finché il ripopolamento non è concluso, la fonte è in stato «in caricamento» e **tutti gli
   indicatori che ne dipendono sono marcati incompleti**: un numero calcolato su uno storico a metà è un numero
   sbagliato.
5. **RF-5** — Il ripopolamento si può richiedere di nuovo a mano, per una fonte già collegata, quando si sospetta
   un buco.
6. **RF-6** — Se il ripopolamento non si conclude entro un tempo ragionevole, la fonte resta «in caricamento» e
   lo dice, con il conteggio di quanto è arrivato: non passa silenziosamente a «completa».

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La richiesta di ripopolamento porta **un solo** `tenant_id`, quello
  dell'account che ha collegato la fonte, preso dal gettone verificato di chi ha fatto il collegamento. Non
  esiste una richiesta di ripopolamento «per tutti gli account»: sarebbe esattamente la scorciatoia da evitare.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/insights/v1/fonti/{app}/ripopolamento` per la
  richiesta manuale; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Lo stato del ripopolamento (richiesto, in corso, concluso, incompleto) sta sulla
  riga `fonte`, con i momenti e i conteggi.
- **RT-4 — Modulo frontend (§3, §5).** La sezione Fonti mostra lo stato di caricamento con il conteggio dei
  fatti arrivati; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Gli stati e i messaggi esistono in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6).** La richiesta manuale richiede ruolo `owner` o `admin`; **non consuma quota**.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: i fatti ripubblicati sono soggetti allo stesso
  contratto di quelli in tempo reale.
- **RT-14 — Registrazione eventi (§14).** «Ripopolamento richiesto», «ripopolamento concluso», «ripopolamento
  incompleto» con `tenant_id`, `app_id` d'origine, `user_id`, conteggi e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Il collegamento chiede il passato**
- **Dato** un account che collega la fonte «fatturazione»
- **Quando** il collegamento avviene
- **Allora** viene pubblicata una richiesta di ripopolamento per quel solo account, su una finestra di trentasei
  mesi, e la fonte passa in stato «in caricamento»

**CA-2 — Lo storico arriva e la fonte si completa**
- **Dato** una fonte in caricamento
- **Quando** arrivano i fatti storici e poi il fatto di chiusura che ne dichiara il conteggio
- **Allora** la fonte passa a «collegata», e gli indicatori che ne dipendono smettono di essere marcati
  incompleti

**CA-3 — Durante il caricamento i numeri sono marcati**
- **Dato** una fonte in caricamento con metà dello storico arrivato
- **Quando** si apre il cruscotto
- **Allora** gli indicatori che dipendono da quella fonte portano il contrassegno di incompletezza **accanto
  alla cifra**, e la scheda del numero dice «fonte in caricamento»

**CA-4 — Ripopolamento che non finisce**
- **Dato** una fonte in caricamento da oltre il tempo massimo previsto, senza fatto di chiusura
- **Quando** si apre la sezione Fonti
- **Allora** la fonte risulta «caricamento incompleto» con il conteggio di quanto è arrivato e la possibilità di
  richiederlo di nuovo — **non** risulta completa

**CA-5 — Isolamento fra account**
- **Dato** due account che collegano la stessa fonte nello stesso momento
- **Quando** i due ripopolamenti si svolgono
- **Allora** ciascun account riceve solo i propri fatti storici, e nessuna richiesta di ripopolamento è
  formulabile per un account diverso da quello che l'ha chiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla macchina a stati del ripopolamento e di **integrazione** sul flusso completo, con
      database effimero e fonte simulata;
- [ ] prova di **isolamento fra account** sulla richiesta e sull'esito;
- [ ] **prova end-to-end**: *rimando* alla storia 0034, che copre il percorso completo; voce `da-coprire` nel
      registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con la finestra di trentasei mesi e il perché;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione nuova; `stato_delle_fonti` (storia 0031)
      espone anche lo stato di ripopolamento;
- [ ] documentazione aggiornata: il dovere di rispondere a una richiesta di ripopolamento riguarda **ogni app
      sorgente** e va scritto nel contratto (storia 0006).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` | i fatti storici arrivano dallo stesso percorso di quelli in tempo reale |
| storia `0008` | è il collegamento a innescare il ripopolamento |
| ogni app sorgente | deve saper rispondere a una richiesta di ripopolamento: è un dovere che nasce qui e vive nelle **loro** storie |

## 7. Fuori ambito

- l'implementazione della risposta al ripopolamento dentro le app sorgenti: è lavoro loro;
- il rilevamento dei buchi in uno storico già caricato: è la storia 0010 (fonte silente) e la 0016
  (completezza del valore).

## 8. Punti aperti

- **Trentasei mesi sono i giusti?** Servono almeno tredici mesi per il confronto con l'anno precedente; trentasei
  consentono di vedere una tendenza. Ma il costo di ripubblicare tre anni di fatti sta **sulla fonte**, non su
  di noi: se una fonte ha molti dati, il ripopolamento può essere pesante. Raccomandazione: finestra
  **configurabile per fonte**, con trentasei mesi come valore predefinito. Chiude: **sviluppatore**.
- **Il dovere di ripopolare è di piattaforma?** Se ogni app deve saper ripubblicare il proprio passato, è un
  requisito che riguarda tutte le app, non solo le fonti di InsightGrove. Va deciso insieme al punto aperto 11
  della descrizione. Chiude: **piattaforma**.

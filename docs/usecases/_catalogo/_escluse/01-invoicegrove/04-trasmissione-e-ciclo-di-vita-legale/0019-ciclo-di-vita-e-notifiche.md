# 0019 — Ciclo di vita e notifiche

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 04 — Trasmissione e ciclo di vita legale
**Storia**: `0019` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`, `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile amministrativo
> voglio aprire l'app e vedere subito, per ogni documento, se è andata o no
> così da non dover controllare tre portali diversi e da sapere cosa richiede la mia attenzione oggi.

**Contesto.** Fino a qui il documento parte; ma il ciclo di vita legale si compie **dopo**, con notifiche che
arrivano in tempi non prevedibili e a volte fuori ordine. È il tratto asincrono che rende questo dominio diverso
da una normale interfaccia di programmazione: una ricevuta di consegna può arrivare dopo un secondo o dopo due
giorni, e la stessa notifica può essere consegnata due volte. Questa storia mette la macchina a stati al riparo da
entrambe le cose e produce la sola risposta che il cliente cerca: «è andata?».

## 2. Requisiti funzionali

1. **RF-1** — Il servizio acquisisce le notifiche dei canali — ricevuta di consegna, mancata consegna, scarto,
   accettazione, rifiuto commerciale — e le registra come `LifecycleEvent` con l'istante dell'autorità e quello di
   acquisizione.
2. **RF-2** — L'acquisizione è **idempotente**: la stessa notifica consegnata più volte produce un solo evento e
   un solo avanzamento di stato.
3. **RF-3** — Una notifica che arriva **fuori ordine** non fa retrocedere lo stato: si registra l'evento, si
   ricalcola lo stato dalla sequenza completa ordinata per istante dell'autorità.
4. **RF-4** — Una notifica per un documento sconosciuto non viene persa: finisce nella coda dei non elaborati
   con il motivo.
5. **RF-5** — La panoramica mostra il riquadro «Cosa richiede attenzione»: documenti fermi oltre una soglia,
   scartati, rifiutati, in mancata consegna.
6. **RF-6** — Il raggiungimento di uno stato definitivo pubblica un **evento in uscita** verso la piattaforma, così
   che l'app di fatturazione e l'incasso crediti sappiano com'è andata.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'`tenant_id` dell'evento si ricava dal documento a cui si riferisce,
  mai dal carico della notifica: una notifica non può spostare un documento fra account. Ogni lettura successiva
  filtra per `tenant_id` preso dal token verificato. Prova di isolamento dedicata a questo punto.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte di sola lettura
  `GET /api/einvoicing/v1/documents/{id}/events` e `GET /api/einvoicing/v1/attention`; l'ingresso delle notifiche
  avviene da un punto d'ascolto dedicato, autenticato con il fornitore e **non** esposto come rotta pubblica di
  dominio. Errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V15__lifecycle_event.sql`: tabella `lifecycle_event` con tipo, codice
  originale, istante dell'autorità, istante di acquisizione, chiave di deduplica; `tenant_id`, chiave UUID
  versione 7, colonne di controllo. Nessuna cancellazione logica: la cronologia di un documento fiscale non si
  cancella.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro «Cosa richiede attenzione» nella panoramica e cronologia sulla
  scheda del documento. Solo token del sistema di design; gli stati usano i colori funzionali; tema chiaro e
  scuro.
- **RT-5 — Cinque lingue (§4).** Nomi degli eventi, degli stati e le frasi del riquadro di attenzione dallo
  spazio-nomi `einvoicing`, presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** L'acquisizione di una notifica **non** consuma quota: il consumo è avvenuto
  alla trasmissione. Va detto esplicitamente, perché il contrario sarebbe un doppio addebito silenzioso.
- **RT-7 — Esposizione conversazionale (§12).** Strumenti dichiarati: `get_document_status(id) → stato, storia
  degli eventi, identificativo dell'autorità` e `list_overdue(periodo?) → documenti fermi oltre la soglia`,
  entrambi **lettura**, nessuna conferma. Sono i due strumenti che rispondono alla domanda «è andata?». Contratto
  dentro il servizio; server conversazionale non implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** `lifecycle_event` può contenere il testo originale della notifica, che cita
  dati del documento: va dichiarata nel manifesto in italiano e inglese e inserita in `exportData` e `purgeData`.
  L'evento in uscita verso la piattaforma **non** deve portare dati personali: porta identificativi e stato.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `notifica acquisita`, `notifica duplicata ignorata`,
  `notifica fuori ordine`, `stato ricalcolato`, `notifica orfana` sono registrati con `tenant_id`, `app_id`,
  `user_id` (di sistema, per le acquisizioni), identificativo di correlazione e tipo — senza il carico.

## 4. Criteri di accettazione

**CA-1 — Avanzamento di stato**
- **Dato** un documento italiano in `in_trasmissione`
- **Quando** arriva la notifica di accettazione con l'identificativo dell'autorità
- **Allora** lo stato diventa `accettato_dall_autorita`, la cronologia mostra l'evento con l'istante
  dell'autorità, e l'identificativo è visibile sulla scheda

**CA-2 — Notifica duplicata**
- **Dato** la stessa notifica consegnata due volte
- **Quando** entrambe sono acquisite
- **Allora** esiste **un solo** evento e lo stato è avanzato una sola volta

**CA-3 — Notifica fuori ordine**
- **Dato** un documento che riceve prima la ricevuta di consegna e poi, in ritardo, l'accettazione con istante
  anteriore
- **Quando** entrambe sono acquisite
- **Allora** lo stato finale è quello corretto secondo la sequenza ordinata per istante dell'autorità, e non
  retrocede

**CA-4 — Notifica orfana**
- **Dato** una notifica che riferisce un documento inesistente
- **Quando** viene acquisita
- **Allora** finisce nella coda dei non elaborati con il motivo, e nulla viene perso

**CA-5 — Cosa richiede attenzione**
- **Dato** un account con un documento scartato e uno fermo da più giorni della soglia
- **Quando** apre la panoramica
- **Allora** entrambi compaiono nel riquadro, con il motivo e il percorso per intervenire

**CA-6 — Isolamento fra account**
- **Dato** una notifica il cui carico contiene l'identificativo di un altro account
- **Quando** viene acquisita
- **Allora** l'evento è associato all'account del documento riferito, e l'identificativo nel carico è ignorato

**CA-7 — Evento in uscita**
- **Dato** un documento che raggiunge uno stato definitivo
- **Quando** l'evento in uscita è pubblicato
- **Allora** contiene identificativi e stato, e **nessun** dato personale

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** su idempotenza, riordino e ricalcolo dello stato; **integrazione** sull'acquisizione con
      il fornitore simulato che consegna doppioni e notifiche fuori ordine;
- [ ] prova di **isolamento fra account** con notifica che tenta di attraversare gli account;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-EINVOICING]` (storia `0030`) attraverserà l'arrivo
      della notifica e la comparsa dello stato definitivo;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con `lifecycle_event`, presente in esportazione e cancellazione;
- [ ] verificato che l'**evento in uscita non porti dati personali**;
- [ ] **registro delle decisioni** compilato, con la scelta «stato ricalcolato dalla sequenza» e il motivo;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `get_document_status` e `list_overdue`.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0017` | Serve la trasmissione italiana e la sua famiglia di stati |
| `0018` | Serve la famiglia a quattro angoli, che ha stati diversi: il ricalcolo deve funzionare su entrambe |

## 7. Fuori ambito

- La **gestione dello scarto**, cioè cosa si fa dopo: storia `0020`. Qui si registra e si mostra.
- La notifica all'utente per posta elettronica quando qualcosa va male: rimandata, come nella storia `0010`, al
  luogo di piattaforma dove si progettano le notifiche in uscita.
- Il consumo dell'evento in uscita da parte delle altre app: è loro, non nostro.

## 8. Punti aperti

- **Chi consuma l'evento in uscita e con quale contratto.** Come per la storia `0012`, il contratto degli eventi
  fra app è una decisione di architettura di piattaforma, non di questa app. Qui si pubblica; se nessuno ascolta,
  non è un difetto di InvoiceGrove.
- **La soglia oltre la quale un documento "è fermo"** dipende dalla giurisdizione e dal canale: in Italia una
  risposta arriva di norma in poche ore, sulla rete può volerci di più. Proposta: soglia dichiarata nel profilo
  della giurisdizione, non costante. Da confermare.

# 0018 — Trasmissione a quattro angoli

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 04 — Trasmissione e ciclo di vita legale
**Storia**: `0018` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0016`, `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che vende anche in Belgio
> voglio mandare la fattura al mio cliente belga dalla stessa schermata da cui mando quelle italiane
> così da non dover comprare e imparare un secondo prodotto per il secondo paese.

**Contesto.** È la seconda realizzazione del contratto (storia `0016`) e serve a due cose insieme: dare all'app la
ragione commerciale per esistere — il multi-paese è l'unico spazio difendibile, descrizione dell'applicazione
§2.1 — e **mettere alla prova il contratto** con una famiglia di ciclo di vita davvero diversa. Nella rete a
quattro angoli non esiste lo stato «accettato dall'autorità»: l'evento rilevante è la **consegna al destinatario**.
Se il contratto della storia `0016` regge qui, regge; se non regge, meglio scoprirlo adesso che alla terza
giurisdizione.

Il Belgio è obbligatorio per tutte le imprese dal 1° gennaio 2026, con un passaggio unico e non scaglionato.
Il costo variabile rilevato è di €0,18–0,25 a documento, **sia in invio sia in ricezione**: un ordine di grandezza
sopra l'Italia, ed è il motivo per cui il piano `europa` della proposta di listino è quello con il margine più
fragile.

## 2. Requisiti funzionali

1. **RF-1** — Esiste l'adattatore a quattro angoli che serializza il documento canonico nel formato della rete e
   lo consegna tramite un punto di accesso certificato.
2. **RF-2** — Il documento passa da `validato` a `in_trasmissione` e poi a `consegnato_al_destinatario`; **non**
   esiste per questa famiglia lo stato `accettato_dall_autorita`, e tentare di portarcelo è rifiutato.
3. **RF-3** — Prima della consegna, l'app verifica che il recapito del destinatario sia registrato e sappia
   ricevere quel tipo di documento (storia `0009`); se la verifica è negativa, la consegna è impedita con la
   spiegazione.
4. **RF-4** — Il destinatario può **rifiutare commercialmente** il documento: l'app registra il rifiuto con il
   motivo, che è cosa diversa da uno scarto per non conformità.
5. **RF-5** — La trasmissione è idempotente e consuma **una** unità della metrica `documenti`, come per l'Italia.
6. **RF-6** — In modalità prova la consegna è negata, come per l'Italia: nulla esce.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Documento e trasmissione filtrano per `tenant_id` preso dal token
  verificato. Prova di isolamento sulla rotta di consegna.
- **RT-2 — Interfaccia di programmazione (§2).** La stessa rotta della storia `0017`
  (`POST /api/einvoicing/v1/documents/{id}/submit`): il canale si sceglie dalla giurisdizione, non
  dall'utente. Errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: si riusa `transmission` della storia `0017`, con canale e
  fornitore diversi. Se emergesse la necessità di una tabella dedicata, è il segnale che il contratto della storia
  `0016` non regge e va corretto lì, non toppato qui.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova: la scheda del documento mostra stati e etichette
  diverse perché la famiglia è diversa, non perché c'è un'interfaccia diversa. Solo token del sistema di design;
  tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I nomi degli stati della famiglia a quattro angoli — `consegnato al
  destinatario`, `rifiutato dal destinatario` — dallo spazio-nomi `einvoicing`, presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Prenotazione di una unità della metrica `documenti` prima della consegna;
  `429` a quota esaurita, `402` con abbonamento non attivo. ⚠️ **La ricezione consuma anch'essa una unità**
  (storia `0021`), perché il fornitore la fattura: va detto al cliente nell'interfaccia, o il conteggio sembrerà
  sbagliato.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: `submit_to_authority` della storia `0017`
  copre entrambe le famiglie. ⚠️ **Il nome dello strumento è però fuorviante per questa famiglia**, dove non c'è
  nessuna autorità: va rinominato in `submit_document` **prima** che il contratto sia pubblico, perché un nome
  stabile non si cambia dopo. Resta marcato **scrittura irreversibile** con conferma umana obbligatoria.
- **RT-8 — Dati personali (§10).** Il carico va a un **secondo fornitore esterno** — il punto di accesso alla
  rete — che è un altro responsabile del trattamento: va dichiarato nell'elenco dei fornitori e nell'informativa,
  con verifica che i dati stiano a riposo in regioni europee. Nessuna tabella nuova nel manifesto, ma la voce del
  fornitore sì.
- **RT-9 — Registrazione eventi (§14).** Gli stessi eventi della storia `0017`, con l'aggiunta di
  `rifiuto commerciale ricevuto`, registrati con `tenant_id`, `app_id`, `user_id`, identificativo di correlazione
  e identificativi, senza carichi né denominazioni.

## 4. Criteri di accettazione

**CA-1 — Consegna riuscita**
- **Dato** un documento verso una controparte belga con recapito verificato e quota disponibile
- **Quando** l'utente conferma la trasmissione
- **Allora** il punto di accesso riceve l'artefatto e lo stato diventa `consegnato_al_destinatario`

**CA-2 — Nessuno stato "accettato dall'autorità"**
- **Dato** un documento della famiglia a quattro angoli
- **Quando** si tenta di portarlo nello stato `accettato_dall_autorita`
- **Allora** la transizione è rifiutata, perché quella famiglia non prevede quello stato

**CA-3 — Recapito non verificato**
- **Dato** una controparte il cui identificativo non risulta registrato
- **Quando** si tenta la consegna
- **Allora** l'operazione è impedita con la spiegazione, nulla esce e nessuna quota è consumata

**CA-4 — Rifiuto commerciale**
- **Dato** un documento consegnato
- **Quando** il destinatario lo rifiuta con un motivo
- **Allora** lo stato diventa `rifiutato_dal_destinatario`, il motivo è visibile, e la diagnosi (storia `0015`) lo
  spiega distinguendolo da uno scarto di conformità

**CA-5 — Stessa rotta, canale diverso**
- **Dato** due documenti, uno italiano e uno belga
- **Quando** si trasmettono entrambi dalla stessa rotta
- **Allora** ciascuno prende il proprio canale, senza che l'utente scelga nulla

**CA-6 — Isolamento fra account**
- **Dato** due account con documenti belgi
- **Quando** un utente dell'uno tenta di trasmettere il documento dell'altro
- **Allora** riceve `404` e nulla esce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance, smoke);
- [ ] **suite di conformità del contratto** (storia `0016`) verde anche su questa realizzazione, **con gli stessi
      documenti di prova** dell'Italia: è la verifica che il contratto regge davvero;
- [ ] prove di **unità** sulla serializzazione nel formato della rete e sulla macchina a stati della famiglia, di
      **integrazione** col punto di accesso **simulato**;
- [ ] prova di **isolamento fra account** e matrice dei ruoli;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-EINVOICING]` (storia `0030`) includerà un documento
      transfrontaliero, che è ciò che distingue questa app dai prodotti nazionali;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per gli stati della famiglia a quattro angoli;
- [ ] **manifesto dei dati** aggiornato con il secondo fornitore esterno dichiarato;
- [ ] **registro delle decisioni** compilato, con il fornitore scelto, il costo a documento e la rinomina dello
      strumento conversazionale;
- [ ] contratto degli **strumenti conversazionali** aggiornato con il nome definitivo.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0016` | Serve il contratto dell'adattatore |
| `0017` | Riusa `transmission`, la rotta di consegna e la prenotazione di quota; la seconda realizzazione deve adattarsi a ciò che la prima ha stabilito, o il contratto va corretto |
| `0009` | La verifica del recapito è un prerequisito della consegna su questa famiglia |
| Contratto con il punto di accesso certificato | Non si diventa punto di accesso: adesione e certificazione costano €1.025–2.750 di iscrizione e €1.800–5.100 l'anno (descrizione dell'applicazione §2.2) |

## 7. Fuori ambito

- L'acquisizione delle notifiche di ricezione: storia `0019`.
- La **ricezione** dei documenti passivi dalla rete: storia `0021`.
- Diventare punto di accesso certificato: escluso, con i numeri in chiaro nella descrizione dell'applicazione.
- La famiglia a cinque angoli (Francia): dichiarata nel contratto, non realizzata.

## 8. Punti aperti

- 🛑 **Il costo negoziato a documento decide se il piano `europa` sta in piedi.** Con €0,25 e un cliente che manda
  tutto sulla rete, il margine si dimezza (descrizione dell'applicazione §5). Serve un preventivo vero prima di
  pubblicare il listino: è la variabile più importante e non l'ho potuta determinare (§2.7).
- **Il nome dello strumento conversazionale** va deciso prima che sia pubblico: `submit_to_authority` è sbagliato
  per questa famiglia. Piccola cosa, ma un nome stabile non si cambia dopo.
- **Se anche la ricezione debba consumare quota.** Tecnicamente sì, perché il fornitore la fattura; dal punto di
  vista del cliente è controintuitivo pagare per ricevere. È una decisione di listino, quindi dello sviluppatore.

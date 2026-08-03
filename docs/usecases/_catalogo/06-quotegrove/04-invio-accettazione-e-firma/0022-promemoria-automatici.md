# 0022 — Promemoria automatici

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 04 — Invio, accettazione e firma
**Storia**: `0022` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`, `0021`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha mandato dodici preventivi questo mese e non ha tempo di richiamarli tutti
> voglio che l'app solleciti da sola chi non ha ancora risposto, con misura
> così da recuperare le offerte che si perdono per dimenticanza, senza diventare insistente.

**Contesto.** «Follow-up automatico sui preventivi senza risposta» è nei casi d'uso principali della scheda di
catalogo, e la ricerca di mercato mostra che ha un valore riconosciuto: Better Proposals lo vende come componente
aggiuntiva a circa dieci dollari per utente al mese (§2.5 della descrizione dell'applicazione). È anche la
funzione che può fare più danni se sbagliata: tre messaggi a un cliente che ha già detto no per telefono
danneggiano la reputazione di chi ci ha dato fiducia.

## 2. Requisiti funzionali

1. **RF-1** — L'account configura una sequenza di promemoria: dopo quanti giorni dall'invio, quanti al massimo, e
   con quale testo (dal modello, per lingua).
2. **RF-2** — I promemoria si fermano da soli quando il preventivo esce dallo stato di attesa: accettato,
   rifiutato, in revisione, scaduto.
3. **RF-3** — Il numero massimo di promemoria per preventivo è **limitato e configurabile con un tetto rigido**
   (proposta: non più di tre); oltre quel numero l'app non manda più nulla, comunque sia configurata.
4. **RF-4** — Ogni preventivo può avere i promemoria **disattivati singolarmente**, con un clic, da chi vende.
5. **RF-5** — Un promemoria mandato **non consuma** una unità della metrica `preventivi_inviati`: la quota si
   consuma sull'offerta, non sull'insistenza.
6. **RF-6** — Si può prevedere un promemoria legato alla scadenza («la tua offerta scade fra tre giorni»), distinto
   da quelli legati al silenzio.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lavorazione periodica opera per account e non attraversa mai due
  account; la configurazione filtra per `tenant_id` dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** `/api/preventivi/v1/impostazioni/promemoria` e
  `POST /api/preventivi/v1/preventivi/{id}/promemoria/sospensione`; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V13__promemoria.sql`: tabella `promemoria_programmato` con
  `tenant_id`, UUID versione 7, colonne di controllo, stato e momento previsto; una riga per promemoria, così che
  il fatto sia tracciabile.
- **RT-4 — Lavorazione periodica.** Idempotente: se gira due volte non manda due volte; se resta indietro,
  recupera senza raffiche. Il fornitore di posta è simulato in locale.
- **RT-5 — Varchi e quota (§6, §7).** I promemoria **non** consumano `preventivi_inviati` (RF-5), ma non partono
  se l'abbonamento non dà accesso (`402`): un account disdetto smette di scrivere ai clienti.
- **RT-6 — Modulo frontend (§3, §5).** Configurazione nella sezione **Impostazioni**; interruttore per singolo
  preventivo; cronologia dei promemoria inviati; solo token del sistema di design; tema chiaro e scuro.
- **RT-7 — Cinque lingue (§4).** L'interfaccia in `en, it, fr, es, de`; i testi dei promemoria sono contenuto del
  cliente, per lingua, come i modelli (storia `0014`).
- **RT-8 — Dati personali (§10).** Nessun campo di persona nuovo rispetto alla storia `0017`; la tabella dei
  promemoria contiene però il riferimento all'invio ed entra in `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** `promemoria programmato`, `promemoria inviato`, `promemoria sospeso` con
  `tenant_id`, `app_id`, `user_id` e correlazione — mai l'indirizzo del destinatario.

## 4. Criteri di accettazione

**CA-1 — Il primo promemoria parte**
- **Dato** un preventivo inviato cinque giorni fa, con sequenza «dopo 5 giorni» · **Quando** passa la lavorazione
  periodica · **Allora** un promemoria è recapitato e la cronologia lo registra

**CA-2 — Si ferma da solo**
- **Dato** un preventivo che il destinatario ha appena visto e rifiutato · **Quando** passa la lavorazione
  periodica · **Allora** nessun promemoria parte, e i successivi in coda sono annullati

**CA-3 — Tetto rigido**
- **Dato** una configurazione che chiedesse dieci promemoria · **Quando** si salva · **Allora** l'app la limita al
  tetto rigido e lo dice, invece di obbedire

**CA-4 — Disattivazione per singolo preventivo**
- **Dato** un cliente che ha già risposto per telefono · **Quando** chi vende disattiva i promemoria su quel
  documento · **Allora** non parte più nulla per quel preventivo, e gli altri non sono toccati

**CA-5 — La quota non si consuma**
- **Dato** un account con quota quasi esaurita · **Quando** partono tre promemoria · **Allora** il consumo di
  `preventivi_inviati` non cambia

**CA-6 — Nessuna raffica dopo un fermo**
- **Dato** la lavorazione ferma per due giorni e venti promemoria in arretrato · **Quando** riparte · **Allora**
  ciascun preventivo riceve al massimo il promemoria che gli spettava, senza duplicati

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sulla programmazione e sull'idempotenza, di **integrazione** con fornitore di posta
      simulato;
- [ ] prova di **isolamento fra account** sulla lavorazione periodica;
- [ ] **prova end-to-end**: rimando — i promemoria dipendono dal tempo e si provano a livello di integrazione;
      motivo e storia proprietaria scritti nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** dell'interfaccia in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la tabella dei promemoria;
- [ ] **registro delle decisioni** compilato (tetto rigido e suo valore, promemoria fuori quota, comportamento
      dopo un fermo);
- [ ] avvio locale invariato; la coda dei promemoria è visibile nella console di amministrazione.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0018` | i promemoria si fermano quando il destinatario risponde |
| storia `0021` | il promemoria legato alla scadenza |

## 7. Fuori ambito

- i promemoria per messaggistica istantanea o telefono: solo posta elettronica, che è il canale già di
  piattaforma; altri canali sarebbero fornitori nuovi;
- i testi generati automaticamente: se arriveranno, arriveranno dal livello conversazionale con conferma.

## 8. Punti aperti

**Il tetto rigido di tre promemoria** è una proposta, non un dato rilevato: nessuna delle fonti consultate indica
un numero di riferimento per il segmento. Se lo sviluppatore preferisce un valore diverso, la storia funziona
uguale — ma un tetto **deve** esistere, perché è ciò che impedisce all'app di diventare uno strumento di molestia.

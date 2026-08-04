# 0020 — Rifiuto e richiesta di modifica

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 04 — Invio, accettazione e firma
**Storia**: `0020` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come cliente che ha ricevuto un'offerta che non mi convince
> voglio poter dire di no, o chiedere una modifica, dalla stessa pagina in cui l'ho letta
> così da non dover scrivere una mail che poi si perde, e da chiudere la faccenda in un momento.

**Contesto.** Un preventivo che resta «inviato» per sempre è rumore: chi vende non sa se aspettare o mollare. Le
due risposte diverse da «sì» servono a chiudere il documento — e la richiesta di modifica, in particolare, è
l'aggancio che riporta la trattativa dentro l'app invece di farla finire in una telefonata. La distinzione fra le
due è anche il dato che alimenta il motivo della perdita (storia `0024`).

## 2. Requisiti funzionali

1. **RF-1** — Dalla pagina pubblica il destinatario può **rifiutare**, indicando facoltativamente un motivo scelto
   da un elenco breve più un testo libero.
2. **RF-2** — Dalla stessa pagina può **chiedere una modifica**, con un testo che spiega cosa vorrebbe.
3. **RF-3** — Il rifiuto porta il preventivo in `rifiutato` e chiude il collegamento; la richiesta di modifica lo
   porta in `in_revisione` e lascia il collegamento leggibile ma non più accettabile.
4. **RF-4** — Chi ha inviato riceve una notifica con il motivo o il testo, e li vede nel backoffice.
5. **RF-5** — Un preventivo rifiutato si può riaprire con una **nuova versione** (storia `0015`): il rifiuto resta
   nella cronologia e non si cancella.
6. **RF-6** — Anche il rifiuto e la richiesta di modifica registrano il loro evento con momento e indirizzo di
   rete, come le altre risposte del destinatario.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1) — via gettone.** Vale quanto detto per la storia `0018`: il gettone abilita
  solo questo preventivo e solo queste risposte.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/preventivi/v1/pubblico/{gettone}/rifiuto` e
  `.../richiesta-modifica`, entrambe idempotenti; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: si usano `evento_preventivo` (storia `0018`) e le colonne di
  stato del preventivo; il testo del destinatario si memorizza sull'evento.
- **RT-4 — Frontend (§3, §5).** Le due azioni sulla pagina pubblica, con conferma; nel backoffice il motivo si
  legge nella cronologia; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** L'elenco dei motivi e i testi in `en, it, fr, es, de`, resi nella lingua del
  destinatario.
- **RT-6 — Dati personali (§10).** Nessun campo di persona nuovo rispetto alla storia `0018`, ma **il testo libero
  scritto dal destinatario** è un contenuto di terzi che finisce nel nostro database: va dichiarato nel manifesto
  come dato imprevedibile e la sua tabella è già in esportazione e cancellazione.
- **RT-7 — Registrazione eventi (§14).** `preventivo rifiutato`, `modifica richiesta` con `tenant_id`, `app_id` e
  correlazione, **senza il testo scritto dal destinatario**.

## 4. Criteri di accettazione

**CA-1 — Rifiuto con motivo**
- **Dato** un preventivo aperto dal destinatario · **Quando** rifiuta scegliendo «prezzo troppo alto» · **Allora**
  lo stato è `rifiutato`, chi ha inviato è avvisato e legge il motivo

**CA-2 — Richiesta di modifica**
- **Dato** lo stesso preventivo · **Quando** il destinatario chiede una modifica scrivendo cosa vorrebbe
- **Allora** lo stato è `in_revisione`, il testo è visibile nel backoffice, e il documento torna modificabile

**CA-3 — Dopo il rifiuto non si accetta**
- **Dato** un preventivo rifiutato · **Quando** si riapre il collegamento e si tenta di accettare · **Allora**
  l'operazione è respinta e la pagina mostra l'esito già registrato

**CA-4 — Riapertura con nuova versione**
- **Dato** un preventivo rifiutato · **Quando** chi vende emette una nuova versione e la invia · **Allora** nasce
  un collegamento nuovo, il rifiuto resta nella cronologia e il documento è di nuovo accettabile

**CA-5 — Idempotenza**
- **Dato** un rifiuto già registrato · **Quando** la richiesta viene ripetuta · **Allora** l'esito è lo stesso e
  non nasce un secondo evento di rifiuto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sulla macchina a stati e di **integrazione** sulle due rotte pubbliche;
- [ ] prova di **isolamento fra account** e prova di sicurezza sul gettone;
- [ ] **prova end-to-end**: rimando alla storia `0030`, che percorre il ramo dell'accettazione; il ramo del
      rifiuto è coperto da prove di integrazione — motivo scritto nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con il testo libero del destinatario;
- [ ] **registro delle decisioni** compilato (elenco dei motivi, effetti sui due stati, riapertura con versione);
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` | condivide la superficie pubblica e la macchina a stati |

## 7. Fuori ambito

- la trattativa vera e propria (scambio di messaggi dentro l'app): non richiesta dal segmento, che usa il telefono;
- il motivo della perdita registrato **da chi vende**, che è una cosa diversa dal motivo dichiarato dal cliente:
  storia `0024`.

## 8. Punti aperti

Nessuno.

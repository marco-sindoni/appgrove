# 0021 — Validità e scadenza dell'offerta

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 04 — Invio, accettazione e firma
**Storia**: `0021` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha quotato materiali il cui prezzo cambia
> voglio che le mie offerte scadano da sole dopo il periodo che ho indicato
> così da non trovarmi un cliente che accetta a marzo un prezzo fatto a novembre.

**Contesto.** La validità è la protezione economica di chi offre, ed è anche un elemento del contenuto minimo che
la legge italiana chiede al preventivo del professionista (costi prevedibili fino alla conclusione dell'incarico,
§2.3 punto 1 della descrizione dell'applicazione). Deve funzionare **da sola**: se la scadenza dipende dal fatto
che qualcuno se ne ricordi, non protegge nessuno.

## 2. Requisiti funzionali

1. **RF-1** — Ogni preventivo ha una data di validità, proposta dal modello (per esempio 30 giorni) e
   modificabile prima dell'invio.
2. **RF-2** — Passata la data, il preventivo passa da solo in `scaduto` e **non è più accettabile**; la pagina
   pubblica lo dice con parole comprensibili.
3. **RF-3** — Chi vende può **prolungare** la validità: è un atto esplicito, tracciato (chi, quando, fino a
   quando), che riporta il documento in stato accettabile senza cambiarne il contenuto.
4. **RF-4** — L'elenco dei preventivi permette di filtrare quelli in scadenza nei prossimi giorni.
5. **RF-5** — La scadenza non cancella niente: il documento e le sue versioni restano leggibili.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il passaggio in `scaduto` è una lavorazione periodica che opera per
  account, senza mai attraversarne due; il prolungamento filtra per `tenant_id` dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/preventivi/v1/preventivi/{id}/prolungamento`; errori
  in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: colonne di validità sul preventivo e una riga di
  `evento_preventivo` per il prolungamento.
- **RT-4 — Lavorazione periodica.** Il passaggio in `scaduto` non si affida al momento della lettura: una
  lavorazione periodica idempotente aggiorna gli stati, e la lettura resta comunque coerente se la lavorazione è
  in ritardo (una offerta scaduta non si accetta anche se lo stato non è ancora stato aggiornato).
- **RT-5 — Modulo frontend (§3, §5).** Indicatore della scadenza nell'elenco e filtro «in scadenza»; azione di
  prolungamento con conferma; solo token del sistema di design; tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Tutte le stringhe visibili in `en, it, fr, es, de`, comprese quelle della pagina
  pubblica.
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-8 — Registrazione eventi (§14).** `preventivo scaduto`, `validità prolungata` con `tenant_id`, `app_id`,
  `user_id` e correlazione.

## 4. Criteri di accettazione

**CA-1 — Scadenza automatica**
- **Dato** un preventivo valido fino a ieri · **Quando** oggi lo si guarda · **Allora** è `scaduto` sia
  nell'elenco sia sulla pagina pubblica

**CA-2 — Non si accetta ciò che è scaduto**
- **Dato** un preventivo scaduto e la lavorazione periodica non ancora passata · **Quando** il destinatario tenta
  di accettare · **Allora** l'operazione è respinta comunque: la verifica è sulla data, non sullo stato scritto

**CA-3 — Prolungamento**
- **Dato** un preventivo scaduto · **Quando** chi vende prolunga la validità di quindici giorni · **Allora** il
  documento è di nuovo accettabile dallo stesso collegamento, il contenuto non è cambiato e il prolungamento è
  nella cronologia

**CA-4 — Filtro in scadenza**
- **Dato** tre preventivi con scadenze diverse · **Quando** si filtra «in scadenza entro sette giorni» · **Allora**
  compaiono solo quelli pertinenti

**CA-5 — Isolamento fra account**
- **Dato** due account con preventivi in scadenza · **Quando** la lavorazione periodica passa · **Allora** ciascun
  documento cambia stato dentro il proprio account e nessuna operazione attraversa i due

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sul calcolo della scadenza e sull'idempotenza della lavorazione, di **integrazione** sul
      prolungamento;
- [ ] prova di **isolamento fra account** sulla lavorazione periodica;
- [ ] **prova end-to-end**: nessun impatto diretto sui percorsi delle storie `0029` e `0030`; il caso «scaduto» è
      coperto da prove di integrazione — motivo scritto nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato (verifica sulla data e non sullo stato, tracciamento del
      prolungamento);
- [ ] avvio locale invariato; la lavorazione periodica è visibile nella console di amministrazione
      (`estensioni-admin.md`).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` | la scadenza è una condizione dell'accettazione |

## 7. Fuori ambito

- il ricalcolo automatico dei prezzi su un'offerta scaduta: pericoloso e non richiesto; si emette una versione
  nuova;
- l'avviso al cliente che l'offerta sta per scadere: è un sollecito, ed è della storia `0022`.

## 8. Punti aperti

Nessuno.

# 0020 — Chiusura del periodo e blocco delle ore

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 04 — Ore lavorate e fatturabilità
**Storia**: `0020` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`, `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che il 3 del mese fattura quello che è stato fatto il mese prima
> voglio poter dire «il mese è chiuso» e che da lì in poi le ore non cambino più
> così da avere un numero su cui fatturare che non si muova sotto i piedi.

**Contesto.** Senza un momento in cui le ore si fermano, il consuntivo di una commessa è un bersaglio mobile: si
fattura 120 ore e il giorno dopo diventano 124 perché qualcuno ha completato la settimana in ritardo. La chiusura
del periodo è la risposta, e va disegnata con una attenzione precisa: **è una decisione sul periodo, non un
giudizio sulle persone**. Per questo FlowGrove non ha un'approvazione delle ore individuale — «il responsabile
approva le tue ore» è esattamente il gesto che trasforma lo strumento in una misura del lavoratore. Si chiude il
mese, non si valuta nessuno.

## 2. Requisiti funzionali

1. **RF-1** — Un periodo (un mese di calendario) si chiude per l'intero account: tutte le righe di ore con data di
   competenza dentro quel periodo passano da `aperta` a `bloccata`.
2. **RF-2** — Una riga `bloccata` non si modifica e non si cancella; si corregge **solo** con una riga di
   rettifica, che porta la data di competenza originale, la differenza (anche negativa), l'autore della rettifica
   e il motivo obbligatorio.
3. **RF-3** — Prima della chiusura l'app mostra un riepilogo: quante ore, di quante persone, su quanti progetti,
   quante non fatturabili, e quali persone non hanno dichiarato nulla nel periodo — senza commenti valutativi.
4. **RF-4** — Un periodo chiuso si può **riaprire** finché nessuna delle sue righe è stata consegnata alla
   fatturazione (storia 0022); la riapertura resta tracciata con autore e motivo.
5. **RF-5** — Chiudere un periodo che ne ha uno precedente ancora aperto avvisa e chiede conferma: i periodi si
   chiudono in ordine, altrimenti le rettifiche diventano ingestibili.
6. **RF-6** — Le persone vedono nel proprio foglio ore che il periodo è chiuso e da quando, e sanno che possono
   chiedere una rettifica.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La chiusura vale per l'account del token verificato e non può toccare
  righe di altri account; l'operazione è verificata riga per riga.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/progetti/v1/periods/{aaaa-mm}/close`,
  `POST /api/progetti/v1/periods/{aaaa-mm}/reopen` e `POST /api/progetti/v1/time-entries/{id}/adjustment`; errori
  in `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V12__periodi.sql`: `period_closure` con `tenant_id`, periodo, stato,
  autore, motivo, colonne di controllo; `time_entry.status` e `time_entry.adjusts_entry_id` per la catena delle
  rettifiche. La chiusura avviene in **una sola transazione** e deve reggere anche su decine di migliaia di righe.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Ore → Periodi*: riepilogo, conferma esplicita, storia delle
  chiusure e riaperture; nel foglio ore, banda che indica il blocco; solo token del sistema di design; tema
  chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Riepilogo, avvisi e messaggi di rettifica in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Ruolo minimo per chiudere e riaprire: `admin`;
  la richiesta di rettifica la può avviare l'autore della riga, l'accettazione è di `admin`.
- **RT-7 — Esposizione conversazionale (§12).** Strumento `close_period(progetto, periodo)`, **scrittura con
  effetto difficilmente reversibile**: bozza con il riepilogo e **conferma umana obbligatoria** (storia 0029). La
  riapertura **non** è esposta come strumento: è un'operazione eccezionale e va fatta guardando lo schermo.
- **RT-8 — Dati personali (§10).** `period_closure.closed_by` e l'autore della rettifica sono dati personali: voci
  nel manifesto in italiano e inglese, campi annotati, tabelle in `exportData` e `purgeData`. Il riepilogo delle
  persone senza ore dichiarate **non si salva**: si calcola al momento e non lascia traccia, perché sarebbe un
  elenco di persone «che non hanno lavorato» ed è esattamente ciò che l'app non vuole produrre.
- **RT-9 — Registrazione eventi (§14).** «Periodo chiuso», «periodo riaperto», «rettifica registrata» con
  `tenant_id`, `app_id`, `user_id`, periodo e numero di righe toccate; mai i motivi scritti a mano.

## 4. Criteri di accettazione

**CA-1 — Chiusura**
- **Dato** un mese con 340 righe di ore aperte
- **Quando** un utente `admin` lo chiude
- **Allora** tutte le 340 righe risultano `bloccate` e il foglio ore delle persone lo mostra

**CA-2 — Modifica impedita**
- **Dato** una riga `bloccata`
- **Quando** l'autore tenta di modificarla dal foglio ore
- **Allora** la risposta è `409` e la cella resta invariata

**CA-3 — Rettifica**
- **Dato** una riga bloccata da 3 ore che in realtà erano 2
- **Quando** si registra una rettifica di −1 ora con motivo
- **Allora** il totale del progetto per quel periodo scende di un'ora, entrambe le righe restano visibili e la
  rettifica porta autore e motivo

**CA-4 — Riapertura impedita dopo la consegna**
- **Dato** un periodo chiuso le cui righe sono già state consegnate alla fatturazione
- **Quando** si tenta la riapertura
- **Allora** la risposta è `409` con la spiegazione, e il periodo resta chiuso

**CA-5 — Chiusura fuori ordine**
- **Dato** il periodo di marzo ancora aperto
- **Quando** si tenta di chiudere aprile
- **Allora** compare l'avviso e la chiusura procede solo con conferma esplicita

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con righe nello stesso mese
- **Quando** `A` chiude il periodo
- **Allora** nessuna riga di `B` cambia stato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`, `compliance`);
- [ ] prove di **unità** sulla catena delle rettifiche e di **integrazione** sulla chiusura transazionale, con un
      volume di righe realistico;
- [ ] prova di **isolamento fra account** sulla chiusura;
- [ ] **prova end-to-end**: coprire ora — `[J-PROGETTI]` chiude il periodo prima di consegnare (storia 0031); voce
      nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato per `period_closure` e per la rettifica;
- [ ] **registro delle decisioni** compilato, con annotata la scelta **chiusura del periodo invece di
      approvazione individuale delle ore** e il perché;
- [ ] controllo automatico di **accessibilità** verde sulla schermata dei periodi;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0017` | Servono le righe di ore e i loro stati |
| Storia `0019` | Il foglio ore deve rispettare il blocco |

## 7. Fuori ambito

- l'approvazione delle ore persona per persona: esclusa per scelta (RF e contesto);
- la chiusura per singolo progetto invece che per account: rimandata; complicherebbe le rettifiche senza un
  beneficio chiaro per un'azienda da cinque persone;
- il blocco automatico a data fissa: la chiusura è una decisione, non un orologio.

## 8. Punti aperti

- **Se un cliente lavorasse a settimane e non a mesi** il periodo mensile potrebbe non bastare. La granularità è
  una scelta di prodotto: prima di renderla configurabile conviene verificare che qualcuno lo chieda davvero.

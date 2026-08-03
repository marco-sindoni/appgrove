# 0002 — Modello dati del salone

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio le tabelle del verticale già isolate per account e già pronte a reggere movimenti immutabili
> così da non scoprire alla terza epica che la giacenza di magazzino non torna e nessuno sa perché.

**Contesto.** Questa storia crea lo scheletro dei dati verticali: le fasi dei servizi, la scheda tecnica, i
prodotti, i movimenti, il conto, i pacchetti, la fedeltà, le provvigioni. Non le riempie di funzioni — lo fanno le
epiche di dominio — ma stabilisce **due scelte strutturali che dopo costano care**: che i movimenti di magazzino
e i movimenti di punti sono **immutabili** (non si modificano, si compensano), e che il conto una volta chiuso non
torna indietro. Sono decisioni da prendere quando la tabella nasce, non quando qualcuno se ne accorge.

## 2. Requisiti funzionali

1. **RF-1** — Esistono le tabelle del verticale con `tenant_id`, chiave primaria UUID versione 7, colonne di
   controllo e cancellazione logica: `fase_servizio`, `variante_servizio`, `scheda_tecnica`, `foto_trattamento`,
   `prodotto`, `giacenza`, `movimento_magazzino`, `dose_prevista`, `conto`, `riga_conto`, `pacchetto`,
   `utilizzo_pacchetto`, `tessera_fedelta`, `movimento_punti`, `regola_provvigione`, `prospetto_provvigioni`.
2. **RF-2** — Le tabelle di **movimento** (`movimento_magazzino`, `movimento_punti`, `utilizzo_pacchetto`) sono
   **immutabili**: non si aggiornano e non si cancellano logicamente; una correzione è un movimento contrario che
   cita quello che corregge.
3. **RF-3** — Il collegamento verso le entità dell'agenda (servizio, risorsa, cliente, prenotazione) è un
   **riferimento logico** — un identificativo, senza chiave esterna — così che la via (a) e la via (b) del §0
   abbiano lo stesso modello e la scelta resti reversibile più a lungo possibile.
4. **RF-4** — Gli importi si conservano in **centesimi interi** e con la valuta accanto; nessun numero a virgola
   mobile per il denaro.
5. **RF-5** — Le quantità di prodotto si conservano con la loro **unità di misura** (millilitri, grammi, pezzi):
   una dose senza unità è un numero che nessuno può sommare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni tabella porta `tenant_id`; ogni interrogazione filtra
  `WHERE tenant_id = :tid` dal token verificato; un `tenant_id` che arrivasse dalla richiesta viene ignorato. La
  suite di isolamento copre **tutte** le tabelle introdotte, non un campione.
- **RT-2 — Persistenza (§8).** Migrazioni Flyway in SQL sotto `db/migration`, schema `app_salone` (via a) o
  `app_prenotazioni` con prefisso `salone_` (via b); nessuna chiave esterna verso altri schemi; nessuna
  applicazione automatica delle migrazioni in produzione.
- **RT-3 — Immutabilità dei movimenti.** Il vincolo non è solo di codice: le tabelle di movimento non hanno
  `updated_at`/`updated_by` valorizzabili dopo l'inserimento e non hanno `deleted_at`. Chi vuole modificarle deve
  incontrare un errore, non una convenzione.
- **RT-4 — Dati personali (§10).** La storia crea le tabelle ma non le popola: le voci del manifesto si scrivono
  **nella storia che introduce il campo**, e la storia `0014` chiude il contratto. Qui si dichiara solo quali
  tabelle **conterranno** dati di persone, così che nessuna sfugga dopo.
- **RT-5 — Registrazione eventi (§14).** Le migrazioni applicate sono registrate con `app_id` e versione; nessun
  contenuto di riga finisce nei registri.
- **RT-6 — Esposizione conversazionale (§12).** Nessuno strumento: non c'è ancora niente da leggere.

## 4. Criteri di accettazione

**CA-1 — Le migrazioni girano da zero**
- **Dato** un database vuoto
- **Quando** si applicano le migrazioni nell'ordine
- **Allora** lo schema esiste con tutte le tabelle, e l'operazione è ripetibile su un database già migrato senza
  effetti

**CA-2 — Isolamento fra account su ogni tabella**
- **Dato** due account `A` e `B` con righe in ciascuna tabella del verticale
- **Quando** un utente di `A` legge qualunque risorsa
- **Allora** vede solo le proprie, anche se forza l'identificativo dell'altro account nella richiesta

**CA-3 — Un movimento non si modifica**
- **Dato** un movimento di magazzino registrato
- **Quando** si tenta di aggiornarlo o di cancellarlo
- **Allora** l'operazione fallisce, e l'unica via offerta è registrare un movimento contrario

**CA-4 — Il denaro non perde centesimi**
- **Dato** un importo di 12,35 €
- **Quando** lo si scrive e lo si rilegge cento volte attraverso somme e sconti
- **Allora** vale ancora esattamente 1.235 centesimi

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh backend` e la suite intera prima del commit;
- [ ] prove di **integrazione** con database effimero e **migrazioni Flyway vere**;
- [ ] prova di **isolamento fra account** su **ogni** tabella introdotta;
- [ ] **prova end-to-end**: *nessun impatto* — non c'è superficie utente;
- [ ] **traduzioni**: non applicabile;
- [ ] **manifesto dei dati**: elenco delle tabelle che conterranno dati di persone dichiarato, voci rimandate alle
      storie che introducono i campi e chiusura alla storia `0014`;
- [ ] **registro delle decisioni** compilato: immutabilità dei movimenti, riferimenti logici verso l'agenda,
      importi in centesimi, unità di misura obbligatoria;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | serve l'impianto e la via scelta: cambia lo schema di destinazione |

## 7. Fuori ambito

- il contenuto funzionale di ciascuna tabella: sta nelle epiche 02-06;
- il manifesto completo dei dati e i diritti dell'interessato: storia `0014`;
- gli indici di ricerca fini: si aggiungono nella storia che introduce la ricerca, dove si sa cosa si cerca.

## 8. Punti aperti

**Il nome delle tabelle sotto la via (b).** Se le tabelle del verticale vivono in `app_prenotazioni` accanto a
quelle dell'agenda, un prefisso (`salone_conto`, `salone_prodotto`) le tiene riconoscibili e rende possibile
un'eventuale estrazione futura. Se lo sviluppatore preferisce nomi puliti, l'estrazione diventa più cara. Proposta:
prefisso sì. Da confermare in fase di implementazione.

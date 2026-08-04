# 0019 — Chiusura del conto

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 05 — Conto, pacchetti e fedeltà
**Storia**: `0019` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0009`, `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come chi sta alla cassa quando la cliente si alza dalla poltrona
> voglio vedere già pronto il conto di quello che le abbiamo fatto, aggiungere lo shampoo che si porta a casa e
> chiudere in dieci secondi
> così da non far aspettare nessuno e da avere, a fine giornata, un incasso che torna senza ricostruirlo.

**Contesto.** È il momento in cui l'appuntamento diventa denaro, ed è il perno di tutto il verticale: **tre effetti
scattano insieme** — il magazzino di cabina si scarica (storia `0017`), le sedute del pacchetto si scalano (storia
`0020`), le provvigioni e i punti maturano (storie `0022` e `0023`). Perché quei tre effetti abbiano senso, la
chiusura dev'essere **un atto solo, atomico e irreversibile**. E perché l'irreversibilità non spaventi, la
rettifica dev'essere ordinaria: se correggere è scomodo, gli operatori smettono di chiudere i conti e l'app perde
il suo dato migliore.

⚠️ **Perimetro da non attraversare**: la chiusura del conto **non è l'emissione di uno scontrino**. Il documento
fiscale passa dal registratore telematico del salone — è la ragione per cui l'app 29 ShopGrove è esclusa dal
catalogo (§1 e §10 della descrizione).

## 2. Requisiti funzionali

1. **RF-1** — Da un appuntamento eseguito si apre un **conto** già compilato con i servizi della sequenza, le
   varianti scelte e l'operatore attribuito a ciascuna riga.
2. **RF-2** — Al conto si aggiungono e si tolgono righe finché è aperto: servizi non prenotati, prodotti di
   rivendita (storia `0021`), sconti a valore o a percentuale con un **motivo** quando superano una soglia
   impostata dal salone.
3. **RF-3** — La chiusura registra il **modo d'incasso dichiarato** (contanti, carta, bonifico, pacchetto, misto)
   e la data, e **non muove denaro**: appgrove non incassa niente per conto di nessuno.
4. **RF-4** — La chiusura è **atomica**: scarico di cabina, decurtazione del pacchetto, maturazione di provvigioni
   e punti avvengono nella stessa transazione. Se una fallisce, non succede niente.
5. **RF-5** — Un conto chiuso **non si riapre e non si cancella**: si corregge con una **riga di rettifica** su un
   documento di rettifica collegato, che compensa gli effetti e resta visibile.
6. **RF-6** — La chiusura emette un **evento** che a valle può diventare ricevuta o fattura nelle app 02 e 01, e
   una riga di incasso nell'app 03. SalonGrove non emette documenti fiscali.
7. **RF-7** — Il totale della giornata si legge in una schermata sola, per modo d'incasso.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Conti, righe e totali filtrano per `tenant_id` dal token verificato; un
  `tenant_id` che arrivasse dalla richiesta viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/<app>/v1/conti`,
  `POST|DELETE /api/<app>/v1/conti/{id}/righe`, `POST /api/<app>/v1/conti/{id}/chiusura`,
  `POST /api/<app>/v1/conti/{id}/rettifiche`; corpo validato (importi non negativi, sconto entro il totale, motivo
  obbligatorio oltre soglia); errori in `problem+json`; OpenAPI aggiornata. **Nessuna rotta di riapertura**.
- **RT-3 — Persistenza (§8).** Tabelle `conto` e `riga_conto` con `tenant_id`, UUID versione 7, colonne di
  controllo e cancellazione logica; importi in **centesimi interi**; macchina a stati `aperto → chiuso` senza
  ritorno, `aperto → annullato` finché è aperto.
- **RT-4 — Atomicità.** Una sola transazione per la chiusura, con i tre effetti dentro. È il requisito tecnico più
  importante della storia e va provato con un guasto simulato in ciascuno dei tre.
- **RT-5 — Comunicazione fra app (§2).** L'evento di conto chiuso è **asincrono**: nessuna chiamata di rete verso
  le app 01, 02 o 03.
- **RT-6 — Varchi e quota (§6, §7).** Funzione accesa dal piano; `402` a piano insufficiente; con abbonamento in
  `past_due` la chiusura resta possibile — un salone in tolleranza deve poter incassare.
- **RT-7 — Modulo frontend (§3, §5).** Il conto si apre dall'agenda con un tocco; la chiusura mostra i tre effetti
  **prima** di confermare («scarico 60 ml di tinta, scalo 1 seduta, maturano 12 € di provvigione»), perché una
  conferma che non dice cosa conferma non è una conferma. Solo token del sistema di design.
- **RT-8 — Cinque lingue (§4).** Modi d'incasso, messaggi di conferma, testi della rettifica in `en, it, fr, es,
  de`.
- **RT-9 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese: `conto.cliente` (economico,
  finalità «il conto del servizio», base «esecuzione del contratto», durata proposta 24 mesi) e
  `riga_conto.operatore` (dato di chi lavora nel salone, base «esecuzione del contratto di lavoro»). Campi
  annotati; tabelle in esportazione e cancellazione (storia `0014`).
- **RT-10 — Esposizione conversazionale (§12).** `apri_conto(prenotazione) → bozza` e
  `aggiungi_riga_conto(conto, voce, operatore) → bozza aggiornata`, entrambi con conferma;
  `chiudi_conto(conto, modo d'incasso) → esito`, **scrittura irreversibile con conferma umana obbligatoria**.
- **RT-11 — Registrazione eventi (§14).** `conto aperto`, `conto chiuso`, `rettifica registrata` con `tenant_id`,
  `app_id`, `user_id`, correlazione e importo — **mai il nome del cliente**.

## 4. Criteri di accettazione

**CA-1 — Il conto nasce già compilato**
- **Dato** un appuntamento eseguito con colore (Sara) e taglio (Marco)
- **Quando** si apre il conto
- **Allora** ci sono due righe con gli importi delle varianti applicate e i due operatori attribuiti

**CA-2 — La conferma dice cosa succede**
- **Dato** un conto pronto per la chiusura, con dosi previste e un pacchetto attivo
- **Quando** si chiede la chiusura
- **Allora** prima di confermare si vede l'elenco dei tre effetti, con i numeri

**CA-3 — Tutto o niente**
- **Dato** una chiusura in cui la decurtazione del pacchetto fallisce
- **Quando** l'operazione termina
- **Allora** il conto è ancora aperto, il magazzino non si è mosso e nessuna provvigione è maturata

**CA-4 — Non si riapre**
- **Dato** un conto chiuso
- **Quando** si tenta di riaprirlo o cancellarlo, anche dall'interfaccia di programmazione
- **Allora** l'operazione fallisce e l'unica via offerta è la rettifica

**CA-5 — La rettifica compensa**
- **Dato** un conto chiuso in cui una riga era sbagliata di 20 €
- **Quando** si registra la rettifica
- **Allora** il totale del giorno cambia di 20 €, la provvigione maturata si corregge di conseguenza, e restano
  visibili sia il conto sia la rettifica

**CA-6 — Sconto oltre soglia senza motivo rifiutato**
- **Dato** una soglia di sconto al 20 %
- **Quando** si applica uno sconto del 30 % senza motivo
- **Allora** l'errore è chiaro e il conto non si chiude

**CA-7 — Isolamento fra account**
- **Dato** due account con conti aperti
- **Quando** un utente del primo chiude un conto forzando l'identificativo di un conto dell'altro
- **Allora** il tentativo è respinto e nulla cambia nell'altro account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (suite intera prima del commit: la storia tocca il perno del verticale);
- [ ] prove di **unità** su totali, sconti e rettifiche; di **integrazione** sull'atomicità con guasto simulato in
      ciascuno dei tre effetti;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** su apertura, chiusura e rettifica;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-SALONGROVE]` (storia `0030`) chiude un conto e verifica
      i tre effetti; registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `conto.cliente` e `riga_conto.operatore`;
- [ ] **registro delle decisioni**: chiusura atomica e irreversibile, rettifica come via ordinaria, nessuna
      emissione di documenti fiscali, nessun movimento di denaro, evento asincrono verso le app 01/02/03;
- [ ] avvio locale invariato; il salone di prova ha un mese di conti chiusi.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0009` | il conto nasce da un appuntamento che può contenere più servizi |
| storia `0017` | lo scarico di cabina è uno dei tre effetti |
| storie `0020`, `0022`, `0023` | gli altri due effetti; si implementano insieme o subito dopo, e finché non ci sono la chiusura ne fa a meno senza rompersi |

## 7. Fuori ambito

- l'emissione dello scontrino o della fattura: perimetro escluso, motivo nel contesto;
- l'incasso vero del denaro: appgrove non muove denaro;
- la prima nota di cassa e la riconciliazione: sono dell'app 03;
- la vendita di prodotti al banco senza appuntamento: storia `0021`.

## 8. Punti aperti

**Quanto è ordinaria la rettifica.** La proposta è che sia a due tocchi dal conto chiuso, con un motivo
obbligatorio e nessuna approvazione. Se lo sviluppatore volesse limitarla a un ruolo, il rischio è quello
descritto nel contesto: i conti smettono di essere chiusi. È una scelta di prodotto, e la segnalo perché ha
conseguenze sulla qualità di tutti i dati a valle.

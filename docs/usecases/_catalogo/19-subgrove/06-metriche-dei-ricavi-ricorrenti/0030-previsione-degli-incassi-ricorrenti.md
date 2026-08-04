# 0030 — Previsione degli incassi ricorrenti

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 06 — Metriche dei ricavi ricorrenti
**Storia**: `0030` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0027`, `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che a ottobre deve decidere se assumere una persona
> voglio vedere quanto mi entrerà nei prossimi mesi da ciò che è **già impegnato**
> così da distinguere il denaro che arriverà comunque da quello che sto solo sperando.

**Contesto.** Il ricavo ricorrente (storia `0027`) guarda indietro e normalizza a mese; questa storia guarda avanti
e guarda alla **cassa**, che è una cosa diversa e va tenuta distinta con cura. Un abbonamento annuale da 240 €
rinnovato a marzo vale 20 € al mese nella misura del ricavo, ma **240 € a marzo** nella previsione degli incassi:
confondere le due letture è l'errore che manda fuori strada chi deve decidere se può permettersi una spesa.

La seconda distinzione, e la più importante, è fra **impegnato** e **sperato**. Impegnato è ciò che discende da
abbonamenti vivi e da condizioni già scritte: il rinnovo di aprile di un abbonamento attivo che non è stato
disdetto. Sperato è tutto il resto: gli iscritti che arriveranno, i rinnovi di chi potrebbe andarsene. L'app
mostra il primo e **si ferma lì**: non fa previsioni statistiche, non estrapola tendenze, non promette numeri che
non può sapere. È una scelta di prodotto e va detta a schermo, perché un titolare che prende per buona una
previsione inventata prende una decisione sbagliata con la nostra firma sopra.

## 2. Requisiti funzionali

1. **RF-1** — L'app mostra, per i prossimi 3, 6 e 12 mesi, l'**incasso impegnato**: la somma delle scadenze già
   generate e di quelle che gli abbonamenti vivi genereranno secondo le condizioni del loro piano, ciascuna nel
   mese in cui è **esigibile**.
2. **RF-2** — La previsione **esclude** ciò che non è impegnato: nessuna stima di iscritti nuovi, nessuna ipotesi
   sui rinnovi di chi ha già disdetto, nessuna proiezione di tendenza. L'assenza è dichiarata a schermo, non
   lasciata intuire.
3. **RF-3** — Ogni mese della previsione distingue tre parti: **già scaduto e incassato**, **già scaduto e non
   incassato**, **atteso**; e dichiara quanti abbonamenti lo compongono.
4. **RF-4** — Gli abbonamenti in `disdetto_a_scadenza` contribuiscono **fino alla fine del periodo** e non oltre;
   quelli `sospeso` non contribuiscono; quelli `in_prova` contribuiscono solo dal primo periodo a pagamento.
5. **RF-5** — La previsione mostra accanto un **margine di prudenza in parole**: quanta parte dell'impegnato, negli
   ultimi mesi, non è poi rientrata (dato reale del conto, non una stima), così che il titolare sappia di quanto
   scontare mentalmente il numero.
6. **RF-6** — La previsione si **esporta** in un file tabellare per mese, con la stessa avvertenza della storia
   `0027`: è una misura di gestione, non un documento contabile.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il calcolo filtra per `tenant_id` preso dal token verificato; nessuna
  aggregazione attraversa account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/abbonati/v1/metriche/previsione?mesi=3|6|12` e
  `.../previsione/esporta`; parametri validati; errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** **Nessuna tabella nuova**: la previsione si calcola dalle scadenze esistenti e dalla
  proiezione del calendario dei rinnovi. Le scadenze future **non si materializzano** in anticipo per fare la
  previsione: le crea la lavorazione giornaliera della storia `0012` quando è il momento, e crearle prima
  produrrebbe documenti fantasma difficili da disfare.
- **RT-4 — Aritmetica del ricorrente.** La proiezione delle date di esigibilità usa **la stessa funzione** di
  calendario della storia `0012`: due implementazioni diverse della stessa aritmetica darebbero previsioni che non
  corrispondono alle scadenze poi generate. È un altro richiamo al punto aperto n. 1 della descrizione.
- **RT-5 — Modulo frontend (§3, §5).** Nella sezione *Andamento*, una serie mensile in avanti con le tre parti
  distinte e la riga sull'impegnato contro lo sperato; disegnata con i soli token del sistema di design; tema
  chiaro e scuro; leggibile anche in sole parole.
- **RT-6 — Cinque lingue (§4).** «impegnato», «atteso», «già scaduto e non incassato», l'avvertenza e le
  intestazioni dell'esportazione in `en, it, fr, es, de`.
- **RT-7 — Varchi e quota (§6, §7).** Lettura: non consuma la metrica `abbonamenti_attivi`. Con abbonamento di
  piattaforma `canceled` risponde `402`; in `past_due` resta accessibile.
- **RT-8 — Esposizione conversazionale (§12).** Contratto dello strumento
  `previsione_incassi(mesi) → per mese: impegnato, scaduto non incassato, atteso, numero di abbonamenti`, marcato
  **lettura**, senza conferma. Il risultato porta **sempre** la marcatura «solo impegnato»: senza di essa un
  assistente presenterebbe il numero come una previsione di fatturato. Contratto raccolto nella storia `0031`.
- **RT-9 — Dati personali (§10).** **Nessun dato personale nuovo**: aggregati per mese; l'apertura sul dettaglio
  riusa gli elenchi di scadenze già esistenti.
- **RT-10 — Registrazione eventi (§14).** `previsione letta (orizzonte)`, `previsione esportata`, con `tenant_id`,
  `app_id`, `user_id` e identificativo di correlazione.
- **RT-11 — Prove (§11).** Unità sulla proiezione dei tre cicli e sugli stati che contribuiscono; prova di
  **coerenza** fra previsione e scadenze effettivamente generate: proiettato un mese, si fa girare la lavorazione
  della `0012` e i due risultati devono coincidere. È la prova che impedisce alle due aritmetiche di divergere.

## 4. Criteri di accettazione

**CA-1 — Cassa, non competenza**
- **Dato** un abbonamento annuale da 240 € che si rinnova il 15 marzo
- **Quando** si guarda la previsione dei prossimi dodici mesi
- **Allora** marzo porta 240 € e gli altri mesi zero, mentre la misura del ricavo ricorrente continua a mostrarne 20
  al mese: la differenza è spiegata a schermo

**CA-2 — Il disdetto non contribuisce oltre la fine**
- **Dato** un abbonamento mensile `disdetto_a_scadenza` con periodo fino al 30 aprile
- **Quando** si guarda la previsione
- **Allora** aprile lo comprende, maggio no

**CA-3 — Impegnato e sperato**
- **Dato** un account in crescita, con tre iscritti nuovi al mese negli ultimi sei mesi
- **Quando** si guarda la previsione
- **Allora** **non** compare alcuna stima di iscritti futuri, e la schermata dichiara che mostra solo l'impegnato

**CA-4 — Tre parti per mese**
- **Dato** il mese in corso con una scadenza incassata, una non incassata e una ancora da maturare
- **Quando** si legge la previsione
- **Allora** le tre parti sono distinte, sommano il totale del mese e ciascuna dice quanti abbonamenti la compongono

**CA-5 — Coerenza con le scadenze vere**
- **Dato** la previsione del mese prossimo
- **Quando** si fa girare la lavorazione dei rinnovi fino a quella data
- **Allora** le scadenze generate corrispondono, per numero e importo, a ciò che la previsione diceva

**CA-6 — Isolamento fra account**
- **Dato** due account · **Quando** uno legge la propria previsione · **Allora** vede solo i propri numeri

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`);
- [ ] prove di **unità** sulla proiezione e sugli stati che contribuiscono; **integrazione** sulla rotta e sulla
      prova di coerenza con la lavorazione dei rinnovi;
- [ ] prova di **isolamento fra account**;
- [ ] **prova end-to-end**: *rimando* — la previsione entra nel percorso `[J-ABBONATI]` della storia `0033`, con
      voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) e storia proprietaria `0033`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, avvertenza compresa;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato: cassa contro competenza, solo impegnato, nessuna materializzazione
      anticipata delle scadenze, riuso dell'aritmetica del calendario;
- [ ] contratto dello strumento `previsione_incassi` dichiarato con la marcatura «solo impegnato»;
- [ ] documentazione aggiornata dove descrive le metriche.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0027` | condivide gli stati che contano e l'avvertenza sulla natura gestionale del dato |
| storia `0012` | la proiezione delle date usa la stessa aritmetica del calendario dei rinnovi |
| storia `0018` | la parte «già scaduto e non incassato» viene dagli esiti registrati |

## 7. Fuori ambito

- qualunque **previsione statistica** (tendenza, stagionalità, probabilità di rinnovo): deliberatamente esclusa,
  vedi la narrazione e il punto aperto;
- la previsione dei **costi** e il flusso di cassa completo: non è di questa app; il quadro trasversale è mestiere
  di **20 InsightGrove**;
- l'incasso vero e proprio: **mai** (§5.2 della [descrizione](../application-description.md)) — l'app registra ciò
  che è dovuto e ciò che è rientrato, il denaro non passa da appgrove.

## 8. Punti aperti

**Se un giorno servirà davvero una previsione «con le probabilità».** La richiesta arriverà, perché il numero
impegnato appare pessimista a chi cresce. Farla bene richiede un modello, dati sufficienti e una spiegazione
onesta dell'incertezza; farla male è peggio che non farla. **Proposta**: restare all'impegnato, mostrare accanto il
margine di prudenza tratto dallo storico reale (RF-5) e riaprire il tema solo con dati veri di clienti veri.
Chiude: lo sviluppatore, con la direzione di prodotto.

**Il trattamento fiscale della cassa.** «Quanto incasso a marzo» non è «quanto fatturo a marzo», e per alcuni regimi
la differenza conta. L'app dichiara di misurare la cassa attesa, ma la parola giusta da usare a schermo davanti a
un commercialista non la so scegliere. Chiude: **revisione legale/fiscale**.

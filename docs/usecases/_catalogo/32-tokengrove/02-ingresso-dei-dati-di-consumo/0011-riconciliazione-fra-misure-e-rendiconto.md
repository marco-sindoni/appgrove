# 0011 — Riconciliazione fra misure e rendiconto

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 02 — Ingresso dei dati di consumo
**Storia**: `0011` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena ricevuto la fattura del fornitore
> voglio sapere quanto il numero di TokenGrove si discosta da quello che pago davvero, e perché
> così da poter usare questi numeri per decidere, invece di ricominciare ogni mese dal foglio di calcolo.

**Contesto.** È la storia che rende il prodotto credibile, e senza di essa TokenGrove è solo «un secondo numero che
non torna». Lo scarto **esiste sempre** e non si può azzerare: dipende da ciò che il cliente non ha strumentato, da
crediti e sconti applicati dal fornitore, da voci che il rendiconto contiene e la misura no (uso di strumenti lato
fornitore, esecuzione di codice), da arrotondamenti e dal cambio di valuta. La scelta di prodotto è dichiararlo
apertamente: un prodotto che mostra il proprio scarto è più credibile di uno che finge di non averlo (rischio R4
del documento capofila).

## 2. Requisiti funzionali

1. **RF-1** — Ogni giorno, per ogni fonte, si calcola lo scarto fra la somma dei costi delle misure attribuite e
   l'importo dichiarato dal rendiconto del fornitore, in valore assoluto e in percentuale.
2. **RF-2** — Lo scarto è **scomposto** nelle sue cause riconoscibili: consumo presente nel rendiconto e non nelle
   misure (non strumentato), misure senza prezzo noto, voci che il rendiconto contiene e il nostro modello non
   prevede, differenza di cambio.
3. **RF-3** — Uno scarto oltre la soglia configurata dalla piattaforma (predefinita 5%) è mostrato come avvertenza
   in testa alla schermata della spesa, non nascosto in un pannello.
4. **RF-4** — La schermata della riconciliazione mostra, per un periodo scelto, la tavola giorno per giorno con:
   somma delle misure, importo del fornitore, scarto, e la scomposizione delle cause.
5. **RF-5** — Il prodotto **non dichiara mai** che i propri numeri coincidono con la fattura: i testi
   dell'interfaccia usano «stima riconciliata con il rendiconto del fornitore» e mai «fattura».
6. **RF-6** — Se il rendiconto per un giorno non è ancora disponibile (il fornitore lo pubblica con ritardo), il
   giorno risulta «in attesa di riconciliazione» e non come «scarto del 100%».

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura di `misura` e `rendiconto` filtra per `tenant_id` preso dal
  gettone verificato; il calcolo dello scarto è per account e non attraversa mai il confine.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/spesa_modelli/v1/riconciliazione` con periodo e
  fonte facoltativa; risposta con la tavola giornaliera e la scomposizione; errori in `problem+json`; definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema `app_spesa_modelli`: tabella `riconciliazione` con
  `tenant_id`, fonte, giorno, somma delle misure, importo dichiarato, scarto, scomposizione, colonne di controllo e
  cancellazione logica. Il valore si calcola una volta e si conserva: ricalcolarlo a ogni lettura su serie lunghe
  sarebbe costoso e darebbe risultati diversi nel tempo.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Spesa», scheda «Riconciliazione»; avvertenza in testa alla spesa
  quando lo scarto supera la soglia; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe, in particolare i nomi delle cause dello scarto, sono presenti
  in `en, it, fr, es, de`. La distinzione fra «stima» e «fattura» va mantenuta nella traduzione: è la ragione per
  cui la traduzione di questa schermata va rivista con attenzione.
- **RT-6 — Esposizione conversazionale (§12).** Lo strumento `stato_fonti` (storia `0006`) restituisce anche lo
  scarto corrente per fonte, marcato **lettura**.
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-8 — Registrazione eventi (§14).** Evento «scarto oltre soglia» con `tenant_id`, `app_id`, fonte, giorno e
  percentuale (non l'importo), con identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Lo scarto è calcolato e scomposto**
- **Dato** un giorno con 1.000 misure inviate e un rendiconto del fornitore che dichiara un importo superiore del 12%
- **Quando** si apre la riconciliazione
- **Allora** il giorno mostra lo scarto in valore e in percentuale, e la scomposizione indica quanta parte è
  attribuita a consumo non strumentato

**CA-2 — Avvertenza sopra la soglia**
- **Dato** uno scarto del 12% e una soglia del 5%
- **Quando** l'utente apre la schermata della spesa
- **Allora** vede l'avvertenza in testa, con il rimando alla riconciliazione

**CA-3 — Rendiconto non ancora pubblicato**
- **Dato** un giorno per cui il fornitore non ha ancora pubblicato il costo
- **Quando** si apre la riconciliazione
- **Allora** il giorno risulta «in attesa di riconciliazione», non con uno scarto del 100%, e nessuna avvertenza
  scatta

**CA-4 — Nessuna promessa di coincidenza con la fattura**
- **Dato** una qualunque delle cinque lingue
- **Quando** si ispezionano i testi della schermata
- **Allora** non compare la parola «fattura» riferita ai nostri numeri, in nessuna lingua

**CA-5 — Isolamento fra account**
- **Dato** due account con rendiconti dello stesso fornitore
- **Quando** uno dei due chiede la riconciliazione
- **Allora** vede solo i propri giorni e i propri importi

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla scomposizione delle cause e di **integrazione** sul calcolo giornaliero, con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla riconciliazione;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «invio parziale, rendiconto
      completo, lo scarto compare», e aggiornare il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con revisione mirata della distinzione stima/fattura;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta di conservare la riconciliazione invece
      di ricalcolarla e sul divieto della parola «fattura»;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0010` | Servono la deduplica e la regola di precedenza fra origini, altrimenti lo scarto sarebbe un artefatto |
| Storia `0014` (epica 03) | Il costo delle misure deve essere calcolato perché ci sia qualcosa da confrontare. Se questa storia viene fatta prima, il confronto si limita alle misure che portano già un importo dal fornitore |

## 7. Fuori ambito

- la **riduzione** dello scarto agendo sull'attribuzione mancante: è l'epica 04, storia `0021`;
- il ricalcolo dei costi passati: è la storia `0017`;
- la conversione di valuta, che è una delle cause dello scarto ma la cui regola è un punto aperto di piattaforma
  (P7 del documento capofila).

## 8. Punti aperti

- **Se e come mostrare lo scarto a chi non ha ancora strumentato nulla.** Un cliente che usa solo il rendiconto ha
  per definizione uno scarto pari al 100% del non attribuito, e mostrarglielo come avvertenza sarebbe un rimprovero
  invece che un'informazione. Proposta: in quel caso la schermata non parla di scarto ma di «quanto vedresti in più
  strumentando», che è la stessa cosa detta come opportunità. È una scelta di prodotto: la chiude lo sviluppatore.

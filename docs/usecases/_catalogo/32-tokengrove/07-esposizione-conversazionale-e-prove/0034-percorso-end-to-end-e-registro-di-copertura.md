# 0034 — Percorso end-to-end e registro di copertura

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0034` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0005`, `0028`, `0032`, `0033`
**Ultimo aggiornamento**: 2026-08-04

## 1. Narrazione

> Come chi manterrà TokenGrove fra sei mesi
> voglio una prova automatica che percorra l'app dal collegamento della fonte fino all'avviso di budget, sullo stack
> locale vero
> così da accorgermi che qualcosa si è rotto **prima** che se ne accorga un cliente guardando un totale sbagliato.

**Contesto.** Questa storia è la **proprietaria del percorso** `[J-SPESA-MODELLI]`: tutte le storie precedenti che
hanno dichiarato «prova end-to-end: si rimanda alla storia `0034`» rimandano qui (`0003`, `0004`, `0010`, `0013`,
`0015`, `0016`, `0018` e le altre), mentre quelle che hanno dichiarato «coprire ora» lo estendono con il proprio
passo. Qui il percorso viene **creato**, ordinato e reso leggibile, e il **registro di copertura**
[docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) viene allineato: registro incoerente =
suite rossa (§11 dei principi di piattaforma).

**Perché serve proprio a quest'app.** Il valore di TokenGrove è la **catena** che va da un dato di consumo grezzo a
un numero in euro attribuito a qualcuno: misura → prezzo datato → costo congelato → attribuzione → budget → avviso.
Ogni anello preso da solo si può provare con una prova d'integrazione; è la **catena intera** che nessuna prova
parziale sorveglia, ed è esattamente lì che un difetto costa la credibilità del numero, che è la sola cosa che
questo prodotto vende.

## 2. Requisiti funzionali

1. **RF-1** — Esiste un percorso end-to-end `tools/platform-e2e/journeys/J-SPESA-MODELLI.spec.ts` in cui **ogni**
   test porta l'etichetta in testa al titolo: `test('[J-SPESA-MODELLI] …')`.
2. **RF-2** — Il percorso principale attraversa, in quest'ordine: accesso all'account con abbonamento attivo →
   comparsa del modulo nella barra laterale → collegamento di una fonte simulata → arrivo delle prime misure →
   comparsa del costo in euro con la versione di catalogo dichiarata → attribuzione tramite etichetta e regola →
   definizione di un budget → superamento di una soglia → avviso recapitato → panoramica che mostra totale,
   copertura e freschezza.
3. **RF-3** — Il percorso comprende i **casi che raccontano le decisioni dell'app**, ciascuno come passo
   verificabile: una misura con il contenuto della richiesta viene **respinta**; lo stesso record inviato due volte
   conta una volta; un modello senza prezzo finisce nel conto separato e non inventa un costo; un ricalcolo produce
   righe nuove e non cambia quelle vecchie; il semaforo resta permissivo quando il servizio è in difficoltà.
4. **RF-4** — Il percorso comprende un passo **conversazionale**: uno strumento di lettura restituisce lo stesso
   totale della schermata, e uno strumento di scrittura produce una bozza che **non** esegue finché non è
   confermata (storie `0032`, `0033`).
5. **RF-5** — Il registro di copertura è aggiornato con le voci di questa app: per ogni storia con superficie
   applicativa, il percorso richiesto e il test che lo copre; per le storie senza superficie, l'esenzione con
   categoria e motivo; per ciò che è rimandato, una voce `da-coprire` con motivo e storia proprietaria.
6. **RF-6** — Il percorso gira **senza dati veri**: fornitore di modelli simulato (storia `0005`), fornitore di
   pagamento simulato, dati inventati, indirizzi in dominio `*.test`, nessuna chiave reale, nessuna chiamata verso
   la rete.

## 3. Requisiti tecnici

- **RT-1 — Prove end-to-end (§11).** Playwright senza finestra sullo stack locale reale; **nessuna attesa a tempo**
  ma attese su condizione; accesso programmatico; dati di prova deterministici e inventati. Il percorso deve poter
  girare due volte di seguito dando lo stesso esito.
- **RT-2 — Isolamento fra account (§1).** Il percorso comprende almeno un passo con **due account** che hanno lo
  stesso periodo e le stesse etichette, e verifica che i totali non si mescolino: è l'invariante numero uno e va
  provato anche da fuori, non solo nelle prove di integrazione.
- **RT-3 — Determinismo del tempo.** Un'app che parla di periodi, previsioni e finestre di budget non può dipendere
  dall'ora in cui gira la prova: il percorso fissa un istante di riferimento e vi ancora i periodi. Una prova che
  fallisce il primo del mese è una prova che nessuno crederà più.
- **RT-4 — Simulazione del fornitore di modelli (§ storia `0005`).** Le fonti in sola lettura e il ricevitore delle
  misure sono serviti dal simulatore locale, che restituisce rendiconti e misure inventate ma **coerenti** fra
  loro: la riconciliazione (storia `0011`) deve poter essere verificata, quindi il simulatore deve saper produrre
  anche uno scarto voluto.
- **RT-5 — Cinque lingue (§4).** Il percorso gira nella lingua predefinita; un passo verifica il **cambio lingua**
  su una schermata dell'app e la presenza delle stringhe tradotte, perché la mancanza di una lingua è un difetto
  che le prove di unità non vedono.
- **RT-6 — Varchi (§6).** Il percorso verifica almeno un varco negato: abbonamento non attivo → `402`, e quota
  esaurita → `429` con il rimedio indicato (storia `0004`).
- **RT-7 — Registro di copertura (§11).** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml)
  aggiornato nello stesso commit del percorso; il controllo `tools/e2e-coverage` (area `tooling` di
  `run-tests.sh`) deve essere verde. Al momento dell'implementazione reale ogni storia di questa cartella avrà
  ricevuto il proprio numero assoluto di use case dalla skill `new-usecase`: è quel numero che entra nel registro,
  non il numero locale di catalogo.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. I dati di prova sono inventati e le etichette usate
  nel percorso non assomigliano a nomi di persone reali.
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo: il percorso osserva il comportamento, non lo cambia.

## 4. Criteri di accettazione

**CA-1 — La catena intera**
- **Dato** lo stack locale avviato con il simulatore del fornitore
- **Quando** gira `[J-SPESA-MODELLI]`
- **Allora** il percorso completa dal collegamento della fonte all'avviso di budget, e ogni passo verifica un esito
  osservabile e non solo l'assenza di errori

**CA-2 — I rifiuti di progetto sono provati**
- **Dato** il passo che invia una misura con il contenuto della richiesta
- **Quando** il ricevitore risponde
- **Allora** la misura è respinta con la spiegazione, e **nulla** del contenuto risulta conservato

**CA-3 — Chat e schermata coincidono**
- **Dato** il passo conversazionale
- **Quando** lo strumento di lettura restituisce il totale del periodo
- **Allora** coincide con quello mostrato dalla panoramica; e la bozza dello strumento di scrittura non produce
  effetti finché non è confermata

**CA-4 — Due account non si mescolano**
- **Dato** due account con misure nello stesso periodo e le stesse etichette
- **Quando** ciascuno apre la propria panoramica
- **Allora** i totali sono quelli propri, e nessuna richiesta forzata restituisce dati dell'altro

**CA-5 — Ripetibile e indipendente dall'ora**
- **Dato** il percorso eseguito due volte di seguito, e una volta con l'istante di riferimento a cavallo di fine
  mese
- **Quando** si guardano gli esiti
- **Allora** sono identici

**CA-6 — Il registro è coerente**
- **Dato** il registro di copertura aggiornato
- **Quando** gira `./run-tests.sh tooling`
- **Allora** il controllo di copertura è verde; se si toglie una voce, diventa rosso

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (suite completa: il percorso tocca backend, frontend e strumenti);
- [ ] percorso `[J-SPESA-MODELLI]` creato, con l'etichetta in testa al titolo di **ogni** test;
- [ ] **registro di copertura** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) allineato,
      con le voci `da-coprire` motivate e assegnate a una storia proprietaria;
- [ ] percorso **ripetibile**: due esecuzioni consecutive con lo stesso esito, nessuna attesa a tempo;
- [ ] nessuna chiamata verso la rete e nessun dato reale nei dati di prova;
- [ ] **traduzioni**: il passo di cambio lingua verifica la presenza delle stringhe tradotte;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, in particolare sull'istante di riferimento fissato e sull'ordine dei
      passi del percorso;
- [ ] documentazione di [docs/testing/README.md](../../../../testing/README.md) aggiornata se il percorso introduce
      una convenzione nuova;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0005` | Serve il simulatore del fornitore di modelli e i dati di prova inventati |
| Storia `0028` | La panoramica è il punto d'arrivo osservabile della catena |
| Storie `0032`, `0033` | Il passo conversazionale prova lettura e bozza-conferma |
| Tutte le storie che hanno dichiarato «coprire ora» | Ciascuna estende questo percorso con il proprio passo: questa storia lo crea e lo tiene ordinato |

## 7. Fuori ambito

- le prove di **unità** e di **integrazione** delle singole storie: restano nelle storie che introducono la logica;
- il percorso end-to-end di **piattaforma** che riguarda l'acquisto dell'abbonamento e l'accensione dell'app: è di
  piattaforma, questo percorso vi si appoggia e non lo riscrive;
- il **livello 3** delle prove di pagamento su ambiente di prova reale: è pre-rilascio e resta fuori dal cancello;
- prove di **carico**: il percorso verifica che le cose funzionino, non quanto reggono. Se servissero, sarebbero
  un'altra storia con altri strumenti.

## 8. Punti aperti

- **Quanto lungo può diventare un solo percorso.** La catena di quest'app è lunga e la tentazione è metterci tutto:
  un percorso che dura troppo viene escluso dai filtri e smette di sorvegliare. Proposta: un percorso principale
  che copre la catena e alcuni percorsi brevi per i casi di rifiuto, tutti con la stessa etichetta. La conferma lo
  sviluppatore insieme a chi cura la suite di piattaforma.

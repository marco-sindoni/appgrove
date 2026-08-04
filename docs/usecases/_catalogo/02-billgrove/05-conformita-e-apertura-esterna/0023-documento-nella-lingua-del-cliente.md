# 0023 — Documento nella lingua del cliente

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 05 — Conformità e apertura verso l'esterno
**Storia**: `0023` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0016`, `0022`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come artigiano italiano che lavora anche per clienti tedeschi
> voglio che la fattura esca in tedesco anche se io la scrivo in italiano
> così da non dover tenere due modelli a parte e da non far arrivare al cliente un documento che non capisce.

**Contesto.** La scheda di catalogo elenca il multi-lingua fra i casi d'uso principali, ed è una funzione che il
mercato di destinazione — globale con priorità europea — rende necessaria appena il cliente attraversa un confine.
La distinzione da tenere chiara è che si tratta di **due lingue diverse**: quella dell'interfaccia, che è l'utente a
scegliere fra cinque, e quella del documento, che dipende dal destinatario. Confonderle è l'errore tipico.

## 2. Requisiti funzionali

1. **RF-1** — Ogni cliente ha una **lingua del documento**; se non è indicata, vale quella predefinita dell'account.
2. **RF-2** — La stampa del documento (storia `0016`) usa la lingua del cliente per tutte le etichette fisse:
   intestazioni, riepiloghi, termini di pagamento, note di legge.
3. **RF-3** — Le parti scritte dall'utente (descrizioni di riga, note) **non** vengono tradotte: restano come sono
   state scritte, ed è giusto così.
4. **RF-4** — Le lingue del documento coincidono con le cinque della piattaforma (`en, it, fr, es, de`); una lingua
   non prevista ricade sull'inglese, e l'interfaccia lo dice.
5. **RF-5** — La lingua del documento viene **congelata** all'emissione, insieme al resto: una ristampa a distanza
   di tempo esce nella stessa lingua di allora.
6. **RF-6** — Il messaggio di sollecito (storia `0019`) usa la stessa lingua del cliente per le sue parti fisse.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lingua predefinita dell'account e quella del cliente sono per
  `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Campo `documentLanguage` sul cliente e sul documento; la rotta di
  stampa non prende la lingua dalla richiesta, la legge dal documento — così una stampa non può uscire in una lingua
  diversa da quella con cui il documento è stato emesso. Definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema `app_billing`: colonna della lingua su `customer` e su
  `document`, con le colonne di controllo consuete.
- **RT-4 — Modulo frontend (§3, §5).** Scelta della lingua sulla scheda del cliente e sul documento in bozza, con
  l'anteprima che la rispetta. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** **Due elenchi da non confondere**: le stringhe dell'interfaccia passano dallo
  spazio-nomi `billing` in `en, it, fr, es, de` (regola di piattaforma); le stringhe **del documento stampato** sono
  un elenco proprio, che vive accanto al modello di stampa e copre le stesse cinque lingue. Mancarne una impedisce
  la conclusione della storia.
- **RT-6 — Varchi e quota (§6).** Nessun consumo di quota.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento proprio. Va però dichiarato che gli strumenti di
  lettura rispondono nella lingua della **conversazione**, non in quella del documento: sono due piani diversi e
  confonderli produce risposte assurde.
- **RT-8 — Dati personali (§10).** La lingua preferita di un cliente persona fisica è un dato che lo riguarda:
  voce nuova nel manifesto in italiano e inglese, campo annotato `@PersonalData`, già coperto da `customer` in
  esportazione e cancellazione.
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo.

## 4. Criteri di accettazione

**CA-1 — Stampa nella lingua del cliente**
- **Dato** un cliente con lingua del documento tedesca e un utente che lavora con l'interfaccia in italiano
- **Quando** si stampa la fattura
- **Allora** le etichette fisse del documento sono in tedesco, mentre l'interfaccia resta in italiano

**CA-2 — Le parti scritte a mano non si traducono**
- **Dato** una riga con descrizione scritta in italiano e cliente tedesco
- **Quando** si stampa · **Allora** la descrizione resta in italiano

**CA-3 — Lingua non prevista**
- **Dato** un cliente in un paese la cui lingua non è fra le cinque
- **Quando** si sceglie la lingua del documento
- **Allora** viene proposto l'inglese e l'interfaccia spiega perché

**CA-4 — Lingua congelata**
- **Dato** una fattura emessa in tedesco e poi il cliente cambiato a francese
- **Quando** si ristampa la fattura · **Allora** esce ancora in tedesco

**CA-5 — Cinque lingue complete**
- **Dato** il modello di stampa · **Quando** si genera il documento in ciascuna delle cinque lingue
- **Allora** nessuna etichetta compare come chiave non tradotta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla scelta della lingua e sul ripiego all'inglese, di **integrazione** sulla stampa nelle
      cinque lingue, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla lingua predefinita;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-BILLING]` usa una lingua sola; la copertura delle cinque è
      nelle prove di integrazione. Proprietaria del rimando: storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, **sia** per l'interfaccia **sia** per il modello di
      stampa;
- [ ] **manifesto dei dati** aggiornato con la lingua preferita del cliente;
- [ ] **registro delle decisioni** compilato, con annotata la distinzione fra lingua dell'interfaccia e lingua del
      documento;
- [ ] contratto degli **strumenti conversazionali**: nessuno proprio, con la nota sulla lingua della conversazione;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0016` | Si traduce ciò che la stampa produce |
| storia `0022` | Valuta e lingua vanno spesso insieme: il cliente estero ha entrambe |

## 7. Fuori ambito

- la traduzione automatica delle descrizioni scritte dall'utente: **esclusa per scelta**, non rimandata: tradurre
  in automatico la descrizione di una prestazione su un documento fiscale è un rischio, non un servizio;
- lingue oltre le cinque della piattaforma: fuori ambito, coerentemente con la regola di piattaforma;
- il documento bilingue su due colonne: rimandato, nessuna evidenza di richiesta.

## 8. Punti aperti

Nessuno.

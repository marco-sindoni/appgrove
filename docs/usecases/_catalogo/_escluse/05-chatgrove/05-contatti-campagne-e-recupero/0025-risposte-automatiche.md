# 0025 — Risposte automatiche

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 05 — Contatti, campagne e recupero
**Storia**: `0025` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio che chiude alle otto
> voglio che chi scrive di notte riceva subito una risposta che dice quando apro
> così da non perdere il cliente che alle undici di sera pensa che io non esista più.

**Contesto.** È la funzione più semplice dell'epica e una delle più utili: il §2.5 dell'analisi individua nel
ritardo di risposta, non nel prezzo, la causa delle vendite perse. Due sole risposte automatiche — il
benvenuto al primo messaggio e il fuori orario — coprono la gran parte del bisogno senza costruire un motore
di automazioni, che è esattamente ciò che il segmento rifiuta. Costa poco anche in denaro: sono risposte
**dentro** la finestra di servizio, quindi gratuite e senza consumo di quota.

## 2. Requisiti funzionali

1. **RF-1** — Il negozio configura due risposte automatiche: **benvenuto** (al primo messaggio di un contatto
   nuovo) e **fuori orario** (quando arriva un messaggio fuori dagli orari di apertura).
2. **RF-2** — Gli orari di apertura si dichiarano per giorno della settimana, con il fuso orario del negozio.
3. **RF-3** — Ogni risposta automatica ha un testo per lingua; si usa la lingua del contatto se nota,
   altrimenti quella predefinita del negozio.
4. **RF-4** — Ogni risposta automatica si può accendere e spegnere singolarmente.
5. **RF-5** — La stessa risposta non si invia più di una volta ogni N ore allo stesso contatto (predefinito:
   24), per non rispondere a raffica a chi manda cinque messaggi di fila.
6. **RF-6** — Le risposte automatiche partono **solo** dentro la finestra di servizio e **non consumano
   quota**; il messaggio automatico compare nel filo, marcato come automatico.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Configurazione, orari e invii filtrano per `tenant_id` preso dal
  token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|PUT /api/chat_commerce/v1/auto-replies` e
  `GET|PUT /api/chat_commerce/v1/business-hours`; corpo validato (orari coerenti, testo non vuoto per la
  lingua predefinita); errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-3 — Varchi e quota (§6, §7).** Le risposte automatiche viaggiano dentro la finestra di servizio e
  quindi **non** prenotano quota: è la ragione per cui possono essere automatiche senza violare la regola
  della conferma umana — non costano denaro e non escono verso chi non ha appena scritto.
- **RT-4 — Persistenza (§8).** Migrazione `V18__risposte_automatiche.sql`: tabelle `auto_reply` e
  `business_hours` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione
  logica; colonna dell'ultima risposta automatica sul contatto per rispettare l'intervallo minimo.
- **RT-5 — Ruoli (§6).** Solo `owner` e `admin` configurano risposte e orari; un `member` riceve `403`.
- **RT-6 — Modulo frontend (§3, §4, §5).** Sezione dentro Impostazioni, con un campo di testo per lingua.
  Attenzione a non confondere due elenchi: l'**interfaccia** è in `en, it, fr, es, de`; le **lingue dei testi
  automatici** sono quelle che il negozio decide di servire e possono essere altre.
- **RT-7 — Dati personali (§10).** Nessun campo nuovo: il testo automatico è scritto dal negozio, la data
  dell'ultima risposta è un metadato sul contatto già coperto dal manifesto.
- **RT-8 — Registrazione eventi (§14).** `risposta automatica inviata` (con il tipo) e `risposta automatica
  saltata per intervallo` con `tenant_id`, `app_id` e identificativo di correlazione, senza contenuti.

## 4. Criteri di accettazione

**CA-1 — Benvenuto**
- **Dato** il benvenuto acceso e un numero mai visto prima
- **Quando** quel numero scrive · **Allora** riceve il testo di benvenuto nella lingua predefinita, e il
  messaggio compare nel filo marcato come automatico, senza consumare quota

**CA-2 — Fuori orario**
- **Dato** orari 9-20 dal lunedì al sabato e il fuori orario acceso
- **Quando** un contatto scrive alle 23:10 di martedì · **Allora** riceve il testo di fuori orario; se scrive
  alle 10:00 di mercoledì, non lo riceve

**CA-3 — Niente raffica**
- **Dato** un contatto che ha appena ricevuto la risposta di fuori orario
- **Quando** manda altri quattro messaggi nella stessa notte · **Allora** non riceve altre risposte automatiche

**CA-4 — Lingua del contatto**
- **Dato** un contatto la cui lingua è il francese e un testo di benvenuto in francese configurato
- **Quando** scrive · **Allora** riceve il testo francese

**CA-5 — Spente**
- **Dato** entrambe le risposte spente · **Quando** arriva un messaggio · **Allora** non parte nulla

**CA-6 — Isolamento fra account**
- **Dato** due account con configurazioni diverse · **Quando** arriva un messaggio a `A`
- **Allora** si applica la configurazione di `A`, mai quella di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo degli orari con il fuso orario e sull'intervallo minimo, e di
      **integrazione** sull'invio automatico con il canale simulato;
- [ ] prova di **isolamento fra account** sulla configurazione e sugli invii;
- [ ] **prova end-to-end**: *nessun impatto* sul percorso principale; il comportamento è coperto dalle prove
      di integrazione, e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) lo dichiara esplicitamente;
- [ ] **traduzioni** dell'interfaccia presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con il motivo per cui una risposta automatica **non** richiede
      conferma umana (dentro la finestra, gratuita, verso chi ha appena scritto);
- [ ] contratto degli **strumenti conversazionali**: la configurazione è esposta in **lettura**; accendere o
      spegnere una risposta automatica è **scrittura con conferma**, perché cambia ciò che i clienti ricevono;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0008` | Serve l'invio in conversazione e la finestra di servizio |

## 7. Fuori ambito

- il menu a scelte automatico («digita 1 per gli orari»): sarebbe un motore di automazioni, escluso;
- la risposta automatica generata da un modello linguistico: appartiene al livello conversazionale di
  piattaforma, non a questa app, e richiederebbe una decisione sulla sorveglianza di ciò che viene detto ai
  clienti finali;
- le festività e le chiusure straordinarie: proposte fuori dalla prima versione.

## 8. Punti aperti

- **Chiusure straordinarie.** Un negozio chiuso una settimana ad agosto vorrà dirlo. È una piccola aggiunta
  agli orari, ma è lavoro che nessuno ha ancora chiesto: annotata qui invece che aggiunta di nascosto.

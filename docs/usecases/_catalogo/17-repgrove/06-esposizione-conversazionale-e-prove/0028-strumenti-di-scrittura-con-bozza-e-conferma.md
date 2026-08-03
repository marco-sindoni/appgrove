# 0028 — Strumenti di scrittura con bozza e conferma

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0028` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0027`, `0014`, `0018`, `0019`, `0021`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che chiede a una chat «rispondi tu a quella recensione di ieri»
> voglio che l'assistente prepari la risposta e me la faccia vedere, e che sia **io** a dire «pubblica»
> così da non trovarmi mai un testo pubblico a nome della mia azienda che non ho letto.

**Contesto.** È il punto dell'app in cui la regola di sicurezza del catalogo pesa più che altrove
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §12): **l'intelligenza artificiale prepara, la persona
approva**. Qui gli effetti non sono soltanto irreversibili in senso tecnico — sono **pubblici e a nome di
qualcun altro**. Una risposta pubblicata resta sulla scheda dell'attività e la vedono i clienti futuri; una
segnalazione inviata a una piattaforma è un atto formale con l'identità del segnalante dentro (storia 0021); un
lotto di inviti spedito è un insieme di messaggi arrivati a persone vere, che non si richiamano.

La storia 0019 ha già messo la conferma **dentro la macchina a stati**, non nell'interfaccia: non esiste
transizione da `bozza` a `pubblicata` senza passare da `approvata` con un utente. Questa storia estende lo stesso
principio a tutte le operazioni comandabili da una chat e lo rende verificabile: non basta che il percorso
automatico non esista nell'interfaccia, deve non esistere **affatto**.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara nel descrittore versionato gli strumenti di **scrittura**: `prepara_risposta`,
   `pubblica_risposta`, `programma_richieste`, `segnala_recensione`. Per ciascuno: nome stabile, descrizione,
   schema dei parametri, schema del risultato, marcatura `scrittura`, indicazione se l'effetto è **irreversibile**
   e obbligo di conferma.
2. **RF-2** — Ogni strumento di scrittura produce una **bozza persistente** e restituisce: cosa verrà fatto, su
   quali elementi, con quale testo esatto, verso quale destinazione, e un riferimento con cui una persona può
   confermare. **Nessuno strumento esegue l'operazione nella stessa chiamata**, nemmeno quando l'utente scrive
   «fallo e basta».
3. **RF-3** — La conferma avviene **fuori dalla conversazione**: una persona con ruolo sufficiente apre la bozza
   nell'app, vede il contenuto definitivo e conferma. La conferma è legata alla singola bozza, scade dopo un tempo
   dichiarato e non è riutilizzabile.
4. **RF-4** — L'esecuzione è **idempotente** rispetto al riferimento della bozza: una conferma ripetuta non
   pubblica due risposte, non manda due lotti di inviti, non invia due segnalazioni.
5. **RF-5** — Per `pubblica_risposta` e `segnala_recensione` la conferma è **obbligatoria e non disattivabile**: non
   esiste impostazione, ruolo, chiave di configurazione o parametro che la tolga. Per `programma_richieste` la
   conferma riguarda il **lotto**, e la bozza mostra chi verrebbe invitato, chi no e con quale motivo — cioè la
   prova di equità prima che parta qualcosa.
6. **RF-6** — Una bozza si può **rifiutare** e scade da sola se nessuno la tocca; una bozza scaduta o rifiutata non
   è più eseguibile e resta come traccia di ciò che è stato proposto e non fatto.
7. **RF-7** — Ogni operazione eseguita registra **chi ha confermato**, quando e da quale bozza: una risposta
   pubblica a nome dell'azienda deve avere un autore umano, ed è la ragione per cui l'app ha il modello a più
   utenti (descrizione §3).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La bozza nasce nell'account del token delegato e si conferma solo con un
  utente dello stesso account; ogni lettura e scrittura filtra per `tenant_id` preso dal token verificato. Una
  bozza di un account non è nemmeno visibile a un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/recensioni/v1/bozze`, `GET …/bozze/{id}`,
  `POST …/bozze/{id}/conferma`, `POST …/bozze/{id}/rifiuta`. L'esecuzione riusa le rotte già esistenti delle storie
  0014, 0019 e 0021: **non** si duplica la logica, altrimenti i due percorsi divergono e uno dei due perde i
  controlli. Errori in `application/problem+json` con codici distinti per «bozza scaduta», «già eseguita», «ruolo
  insufficiente»; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__bozza_operazione.sql` sullo schema `app_recensioni`: tabella
  `bozza_operazione` con `tenant_id`, tipo di operazione, contenuto proposto, elementi coinvolti, stato
  (`proposta`, `confermata`, `eseguita`, `rifiutata`, `scaduta`), scadenza, chi ha proposto (utente delegante),
  chi ha confermato, esito. Chiave primaria a identificativo universale versione 7, colonne di controllo,
  `deleted_at`. Il contenuto proposto è **immutabile**: si conferma esattamente ciò che si è visto.
- **RT-4 — Modulo frontend (§3, §5).** Sezione trasversale «Da confermare» nel modulo, raggiungibile da un
  indicatore nella barra: elenco delle bozze in attesa, scheda con il testo definitivo e la destinazione, pulsanti
  di conferma e di rifiuto. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe — comprese le descrizioni degli strumenti e i messaggi di
  rifiuto — in `en, it, fr, es, de` sotto lo spazio-nomi `recensioni`.
- **RT-6 — Varchi e quota (§6, §7).** La conferma richiede ruolo `admin` o `owner` per la pubblicazione e per la
  segnalazione; `402` con abbonamento non attivo. `programma_richieste` non consuma la metrica `sedi_monitorate`
  (che è a giacenza sulle sedi), ma l'esecuzione del lotto rispetta i limiti del canale di invio della storia 0014.
- **RT-7 — Esposizione conversazionale (§12).** È l'oggetto della storia. Il descrittore dichiara per ciascuno
  strumento `richiede_conferma: true` e, per i due irreversibili, `disattivabile: false`. Il server conversazionale
  è di piattaforma e non esiste (UC 0061-0063): la conferma è quindi implementata **nel servizio dell'app**, così
  che continui a valere qualunque cosa faccia il livello di sopra.
- **RT-8 — Dati personali (§10).** Il contenuto della bozza di `programma_richieste` contiene **nomi e recapiti**
  dei clienti finali: la tabella `bozza_operazione` va dichiarata nel manifesto in italiano e inglese, i campi
  annotati `@PersonalData`, e la tabella aggiunta a `exportData` e `purgeData` (storia 0031). Le bozze scadute si
  cancellano secondo la stessa conservazione dei dati d'origine, non «per sempre».
- **RT-9 — Registrazione eventi (§14).** `bozza proposta`, `bozza confermata`, `bozza rifiutata`, `bozza scaduta`,
  `esecuzione riuscita`, `esecuzione fallita`, con `tenant_id`, `app_id`, `user_id` di chi conferma, tipo di
  operazione e identificativo di correlazione. **Mai** il testo proposto né i recapiti.

## 4. Criteri di accettazione

**CA-1 — Nessuna esecuzione senza conferma**
- **Dato** un assistente che invoca `pubblica_risposta` con qualunque combinazione di parametri, compreso un
  parametro inventato che sembri una conferma
- **Quando** la chiamata viene eseguita
- **Allora** si ottiene una bozza, nulla risulta pubblicato sulla piattaforma simulata e la risposta resta nel suo
  stato precedente

**CA-2 — La conferma è di una persona**
- **Dato** una bozza di pubblicazione in attesa
- **Quando** un utente con ruolo `admin` la apre nell'app e conferma
- **Allora** l'operazione viene eseguita, e risulta registrato **chi** ha confermato e da quale bozza

**CA-3 — Ruolo insufficiente**
- **Dato** una bozza di segnalazione e un utente `member`
- **Quando** tenta di confermare
- **Allora** riceve `403`, la bozza resta `proposta` e nulla viene inviato

**CA-4 — Conferma ripetuta**
- **Dato** una bozza già eseguita
- **Quando** si ripete la conferma
- **Allora** l'esito è lo stesso della prima volta e non viene eseguita una seconda operazione

**CA-5 — Bozza scaduta**
- **Dato** una bozza oltre la propria scadenza
- **Quando** si tenta di confermarla
- **Allora** l'operazione è rifiutata con il codice «bozza scaduta» e resta la traccia di ciò che era stato
  proposto

**CA-6 — Il lotto di inviti mostra anche le esclusioni**
- **Dato** un periodo con clienti serviti di cui alcuni non ammissibili
- **Quando** si invoca `programma_richieste`
- **Allora** la bozza elenca chi verrebbe invitato **e** chi no con il motivo, e nulla parte finché non si conferma

**CA-7 — Isolamento fra account**
- **Dato** una bozza dell'account `A`
- **Quando** un utente di `B` tenta di leggerla o confermarla
- **Allora** riceve `404` e nulla accade

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla macchina a stati della bozza, con la verifica **esplicita** che non esista alcun
      percorso da `proposta` a `eseguita` senza un utente confermante; di **integrazione** su ciascuno strumento con
      piattaforme e fornitore di recapito **simulati**;
- [ ] prova di **isolamento fra account** su bozze e conferme;
- [ ] **prova end-to-end**: *coprire ora* il percorso «bozza da assistente → tentativo di esecuzione senza conferma
      → conferma → esecuzione» dentro `[J-RECENSIONI]` (storia 0030), e registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la tabella `bozza_operazione`, i suoi campi personali e la sua
      conservazione;
- [ ] **registro delle decisioni** compilato, con la scelta di implementare la conferma nel servizio e non nel
      livello conversazionale, e con i tempi di scadenza delle bozze;
- [ ] contratto degli **strumenti conversazionali**: quattro strumenti di scrittura dichiarati, due marcati
      irreversibili e non disattivabili.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0027` | il descrittore e le regole comuni del contratto nascono lì |
| storie `0014`, `0019`, `0021` | sono le operazioni che gli strumenti mettono in bozza; l'esecuzione riusa le loro rotte |
| storia `0018` | `prepara_risposta` è esattamente la bozza già introdotta lì |
| UC 0061-0063 (livello conversazionale) | non implementati: la conferma vive nell'app, così vale comunque |

## 7. Fuori ambito

- l'interfaccia di conversazione con cui l'utente vede la bozza: è di piattaforma; qui si espone il contenuto da
  mostrare;
- il rifiuto delle richieste che chiedono pratiche vietate — storia 0029;
- la conferma per operazioni che non escono verso l'esterno (per esempio cambiare il numero di recensioni mostrate
  nel riquadro): non sono strumenti di scrittura in questa versione;
- l'esecuzione differita nel tempo di una bozza confermata («pubblica domani mattina»): rimandata, aggiunge una
  finestra in cui il contenuto confermato e il mondo divergono.

## 8. Punti aperti

- **La durata di validità di una bozza** (proposta: 48 ore per la pubblicazione, 7 giorni per il lotto di inviti,
  che ha una finestra normativa propria — storia 0015). È una scelta di prodotto: **da confermare.**
- **Se la conferma debba poter avvenire anche da una notifica sul telefono** invece che dentro l'app: comodo, ma
  sposta il punto in cui la persona vede il testo definitivo. Rimandato, e va deciso guardando come il livello
  conversazionale di piattaforma presenterà le bozze (UC 0063).

# 0034 — Percorso end-to-end dell'abbonato

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0034` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0023`, `0024`, `0025`, `0026`, `0033`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore responsabile di una pagina che una legge obbliga a tenere semplice
> voglio una prova che percorra la disdetta esattamente come la vive l'abbonato, dal collegamento nella posta
> così da sapere che nessuna modifica futura potrà aggiungere di nascosto un passaggio o una richiesta di accesso.

**Contesto.** La superficie pubblica dell'epica 05 è l'unica parte di SubGrove dove un **obbligo di legge** si
traduce in un comportamento misurabile: il pulsante di disdetta dev'essere raggiungibile **senza credenziali** e
senza pratiche dilatorie (§ 312k del codice civile tedesco; per l'Italia, canale digitale di recesso semplice
quanto l'adesione — §2.3 della [descrizione](../application-description.md)). Un obbligo di questo tipo non si
presidia con una raccomandazione nel codice: si presidia con una prova **scritta per rompersi** se qualcuno
aggiunge un passaggio, una domanda obbligatoria o una richiesta di accesso. È la differenza fra sperare che nessuno
lo faccia e accorgersene lo stesso giorno.

Il percorso è **separato** da quello interno della storia `0033` per una ragione sostanziale: è l'unico che gira
**senza sessione**, fuori dal backoffice, con l'unica autorizzazione del gettone firmato. Mescolarlo con l'altro
significherebbe eseguirlo con un utente già autenticato — cioè non provare la cosa che conta.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il percorso `tools/platform-e2e/journeys/J-ABBONATI-PUBBLICO.spec.ts`, eseguito senza
   finestra sullo stack locale reale, con ogni prova etichettata `[J-ABBONATI-PUBBLICO]` in testa al titolo.
2. **RF-2** — Il percorso parte dal **messaggio vero**: il collegamento si prende dall'avviso di rinnovo arrivato
   nella casella di posta locale di collaudo, non da un indirizzo costruito dalla prova. È l'unico modo per
   verificare che la catena messaggio → pagina regga davvero.
3. **RF-3** — Il percorso apre la pagina **in un contesto senza sessione** (nessun gettone di accesso, nessuna
   memoria del browser) e verifica che mostri piano, canone, periodo, prossimo rinnovo, ultimo giorno utile per
   disdire e l'elenco delle scadenze — e nient'altro.
4. **RF-4** — Il percorso **disdice** e verifica le tre cose che l'obbligo richiede: la disdetta si compie in
   **due interazioni**, **in nessun passaggio** viene chiesta una credenziale, e la ricevuta scritta arriva al
   recapito dell'abbonato.
5. **RF-5** — Il percorso verifica che la domanda sul **motivo** compaia **dopo** la conferma e che chiudere senza
   rispondere lasci la disdetta valida.
6. **RF-6** — Il percorso verifica le **difese** della storia `0026`: un gettone scaduto e un gettone inesistente
   producono la stessa pagina neutra, e nessuna delle due rivela se un abbonamento esiste.
7. **RF-7** — Il percorso verifica la pagina **su schermo stretto** (larghezza da telefono) e in **tema scuro**:
   nessuno scorrimento orizzontale, contrasti leggibili, pulsante di disdetta visibile senza cercarlo; il controllo
   automatico di accessibilità è verde.
8. **RF-8** — Il registro di copertura
   [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve la voce del percorso
   `J-ABBONATI-PUBBLICO` con gli use case coperti, e le voci `da-coprire` delle storie `0023`-`0026` che questo
   percorso chiude vengono tolte.

## 3. Requisiti tecnici

- **RT-1 — Prove end-to-end (§11).** Playwright senza finestra, stack locale reale, nessuna attesa a tempo, dati
  deterministici e inventati (storia `0005`). L'etichetta `[J-ABBONATI-PUBBLICO]` in testa al titolo è ciò che il
  controllo `tools/e2e-coverage` cerca.
- **RT-2 — Contesto senza sessione.** Il percorso usa un contesto di browser pulito, senza credenziali e senza
  memoria condivisa con il percorso interno. Una prova che partisse da un utente autenticato non proverebbe
  l'obbligo: lo aggirerebbe.
- **RT-3 — La prova che si rompe di proposito.** L'asserzione «due interazioni, nessuna credenziale» va scritta in
  modo **fragile a bella posta**: conta i passaggi e cerca l'assenza di campi di accesso lungo tutto il tragitto,
  così che aggiungere un enigma anti-robot, una domanda obbligatoria o un'offerta di trattenimento faccia diventare
  rossa la suite. È il presidio di conformità della storia `0024` reso automatico.
- **RT-4 — Isolamento fra account (§1).** Il percorso verifica che un gettone dell'account `A` non risolva su un
  abbonamento dell'account `B`, e che la risposta sia quella neutra.
- **RT-5 — Cinque lingue (§4).** Il percorso apre la pagina almeno in **due** lingue, di cui il tedesco, e verifica
  che la dicitura del pulsante di disdetta sia quella prevista per quella lingua e non una chiave né una
  traduzione mancante. La correttezza giuridica della dicitura resta un punto aperto della storia `0024`: qui si
  verifica che la lingua giusta arrivi a schermo.
- **RT-6 — Comunicazioni.** Avviso di rinnovo e ricevuta di disdetta si leggono dalla casella di posta locale di
  collaudo, come negli altri percorsi di piattaforma.
- **RT-7 — Dati personali (§10).** **Nessun dato personale nuovo**: dati inventati, ambiente locale. Il percorso
  verifica però una regola di trattamento: che la pagina **non** mostri note interne, altri abbonamenti dello
  stesso abbonato né riferimenti dell'autorizzazione all'addebito.
- **RT-8 — Avvio locale (§15).** Il percorso si appoggia alla scoperta automatica dei servizi; nessun passo manuale
  prima di eseguirlo.

## 4. Criteri di accettazione

**CA-1 — Dalla posta alla pagina**
- **Dato** un abbonamento con avviso di rinnovo appena inviato
- **Quando** la prova prende il collegamento dal messaggio e lo apre in un contesto senza sessione
- **Allora** la pagina mostra piano, canone, periodo, prossimo rinnovo, ultimo giorno utile e scadenze, e nient'altro

**CA-2 — Disdetta in due interazioni, senza credenziali**
- **Dato** la pagina aperta · **Quando** la prova preme il pulsante e conferma
- **Allora** l'abbonamento risulta `disdetto_a_scadenza`, i passaggi sono stati due, e in nessun momento è comparso
  un campo di accesso

**CA-3 — La prova si rompe se qualcuno aggiunge un ostacolo**
- **Dato** una modifica che introduce una domanda obbligatoria prima della conferma
- **Quando** si esegue il percorso
- **Allora** la prova fallisce e il messaggio dice che il percorso di disdetta si è allungato

**CA-4 — Ricevuta e motivo facoltativo**
- **Dato** una disdetta confermata
- **Quando** si guarda la casella di collaudo e la schermata successiva
- **Allora** la ricevuta è arrivata con la data di fine copertura, e chiudendo senza rispondere al motivo la
  disdetta resta valida

**CA-5 — Difese**
- **Dato** un gettone scaduto e uno inesistente
- **Quando** si aprono le rispettive pagine
- **Allora** la pagina è la stessa, neutra, e non dice se un abbonamento esiste

**CA-6 — Telefono e tema scuro**
- **Dato** una finestra larga 390 punti in tema scuro
- **Quando** si apre la pagina e si percorre la disdetta
- **Allora** non c'è scorrimento orizzontale, il pulsante è visibile senza cercarlo e il controllo di accessibilità
  è verde

**CA-7 — Registro coerente**
- **Dato** il registro di copertura aggiornato · **Quando** gira `./run-tests.sh tooling`
- **Allora** il controllo è verde e nessuna voce `da-coprire` resta per le storie `0023`-`0026`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (intera suite prima del commit: il percorso tocca `platform` e
      `tooling`);
- [ ] percorso `[J-ABBONATI-PUBBLICO]` scritto e stabile su tre esecuzioni consecutive;
- [ ] prova di **isolamento fra account** sul gettone dentro il percorso;
- [ ] **registro di copertura** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml)
      aggiornato: voce del percorso, use case coperti, voci `da-coprire` delle storie `0023`-`0026` chiuse;
- [ ] **traduzioni**: verificata la resa in almeno due lingue, tedesco compreso;
- [ ] **manifesto dei dati**: nessuna voce nuova; verificata l'assenza a schermo dei dati che non devono comparire;
- [ ] **registro delle decisioni** compilato: percorso separato senza sessione, asserzione fragile a bella posta,
      voci di registro chiuse;
- [ ] controllo automatico di **accessibilità** verde sulla pagina pubblica e sul percorso di disdetta;
- [ ] documentazione di collaudo aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0023` | la pagina e il gettone |
| storia `0024` | la disdetta, che è ciò che il percorso presidia |
| storia `0025` | la richiesta di cambio piano, verificata nel passaggio finale |
| storia `0026` | le difese: gettone scaduto e risposta neutra |
| storia `0033` | riusa l'impianto del percorso interno (creazione dell'account, dati, casella di collaudo) |

## 7. Fuori ambito

- il percorso **interno** del cliente: storia `0033`;
- la verifica **giuridica** della dicitura del pulsante per giurisdizione: è un punto aperto della storia `0024`,
  che chiude la revisione legale — una prova automatica può verificare che la lingua giusta arrivi a schermo, non
  che il testo sia conforme;
- le prove di carico sulla superficie pubblica: non sono di questa storia; il limite di frequenza è provato a
  livello di integrazione nella `0026`;
- l'iscrizione autonoma di chi non è ancora abbonato: non esiste (punto aperto della storia `0010`).

## 8. Punti aperti

**Come si misura «due interazioni» senza rendere la prova fragile per i motivi sbagliati.** Contare i clic è
grossolano: una modifica innocua all'aspetto può cambiarne il numero. **Proposta**: contare i **passaggi di
schermata** dalla pagina all'esito e verificare l'assenza di campi di accesso e di moduli obbligatori lungo il
tragitto, invece di contare i clic. Chiude: lo sviluppatore.

**Quale lingua rappresenta l'obbligo.** Il percorso verifica il tedesco perché è l'unica lingua con una formula
imposta che abbiamo trovato. Se la revisione legale rilevasse obblighi analoghi in altre giurisdizioni, il percorso
va esteso. Chiude: **revisione legale**, poi lo sviluppatore.

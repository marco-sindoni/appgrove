# 0035 — Strumenti di scrittura con bozza e conferma

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0035` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0018`, `0034`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che detta alla chat mentre è in movimento
> voglio poter preparare una campagna e onorare una disiscrizione arrivata a voce, senza che nulla parta o cambi
> prima che io abbia detto sì
> così da usare la comodità della chat senza il rischio che un fraintendimento diventi un messaggio a duemila
> persone.

**Contesto.** È la storia in cui la regola di sicurezza del catalogo — l'intelligenza artificiale prepara, la
persona approva — smette di essere un principio e diventa codice. In questa app la posta in gioco è più alta che
altrove: gli effetti sono **verso l'esterno e irreversibili**, perché un messaggio inviato non si richiama, e
riguardano persone che non sono clienti nostri. Per questo `programma_invio` è progettato in modo che **non
programmi**: esegue il controllo pre-volo (storia 0018) e restituisce l'esito, e la programmazione vera resta un
atto separato che una persona compie.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara tre strumenti di **scrittura**: `crea_bozza_di_campagna`, `programma_invio`,
   `disiscrivi`. Ognuno dichiara nome stabile, descrizione, schema dei parametri, schema del risultato e la
   marcatura **scrittura**, con l'indicazione se l'effetto è reversibile.
2. **RF-2** — `crea_bozza_di_campagna(nome, segmento, modello o testo, canale)` crea una campagna nello stato
   `bozza`, che per costruzione **non può partire da sola**: nessun parametro dello strumento può portarla oltre
   quello stato.
3. **RF-3** — `programma_invio(id_campagna, momento)` **non programma**: esegue il controllo pre-volo e
   restituisce l'esito completo — dominio mittente verificato, destinatari inviabili, esclusi con il motivo,
   invii che saranno consumati dalla quota — insieme a una richiesta di conferma esplicita. La programmazione
   avviene solo con una seconda chiamata che porta il riferimento della conferma.
4. **RF-4** — `disiscrivi(recapito, motivo)` registra una revoca ed è **irreversibile**: richiede sempre conferma,
   e la conferma dice quale recapito e quali canali verranno toccati. Serve al caso reale in cui l'opposizione
   arriva a voce o al telefono, e deve essere onorata subito.
5. **RF-5** — Ogni conferma ha una validità breve e vale **una volta sola**: una conferma scaduta o già usata non
   produce effetti, e lo strumento risponde chiedendo di rifare la verifica. Serve a evitare che un sì detto per
   un'altra cosa autorizzi un invio dieci minuti dopo.
6. **RF-6** — `registra_consenso` e `importa_lista` **non sono esposti** alla chat, e la motivazione è scritta nel
   contratto: il primo è una dichiarazione con valore probatorio e dev'essere un atto compiuto da una persona
   nell'interfaccia, dove vede esattamente cosa sta dichiarando; il secondo perché un'importazione fatta «a voce»
   è precisamente il modo in cui nascono le liste di cui nessuno sa più l'origine.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Gli strumenti operano nell'account del chiamante autenticato; nessuno
  schema contiene un parametro di account. `disiscrivi` cerca il recapito **solo** dentro l'account del chiamante:
  un recapito che esiste in un altro account riceve la stessa risposta di un recapito inesistente.
- **RT-2 — Interfaccia di programmazione (§2).** Contratto nel pacchetto `app.appgrove.campaigns.tools`,
  versionato con il servizio; gli strumenti riusano i servizi applicativi esistenti — la stessa macchina a stati
  della campagna e lo stesso controllo pre-volo dell'interfaccia — e non scorciatoie proprie. Errori in
  `application/problem+json`.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__add_tool_confirmation.sql` sullo schema `app_campaigns`: tabella
  `tool_confirmation` con `tenant_id`, chiave primaria UUID versione 7, strumento, riepilogo di ciò che si sta per
  fare, scadenza, momento e identificativo dell'uso, colonne di controllo e cancellazione logica. È la prova che
  un effetto irreversibile è stato autorizzato da una persona: si aggiunge, non si modifica.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova. Le campagne create dalla chat compaiono
  nell'elenco del modulo `campaigns` marcate come «create dall'assistente», perché chi apre l'app deve capire da
  dove viene una bozza che non ricorda di aver scritto.
- **RT-5 — Cinque lingue (§4).** Le descrizioni degli strumenti sono in inglese (sono per un modello
  linguistico); i **testi di riepilogo delle conferme** vengono restituiti nella lingua dell'utente e sono
  presenti in `en, it, fr, es, de`: è il testo che una persona legge prima di autorizzare un effetto
  irreversibile, e non può arrivare in una lingua che non è la sua.
- **RT-6 — Varchi e quota (§6, §7).** Ogni chiamata attraversa la catena dei varchi: `401`, `403`, `402`. La
  verifica di `programma_invio` **non** consuma la metrica `messages_sent` (natura `flow`), ma **dichiara** quante
  unità servirebbero e, se non bastano, risponde con l'esito «quota insufficiente» e il rimedio invece di
  chiedere una conferma che non potrebbe essere onorata. Il consumo avviene alla programmazione confermata, come
  per l'interfaccia.
- **RT-7 — Esposizione conversazionale (§12).** È la storia che realizza la regola «scrittura con bozza e conferma
  umana». Dipendenza dichiarata: UC 0061-0063, non ancora implementati; finché non esistono, il ciclo
  bozza-conferma si prova chiamando il contratto direttamente.
- **RT-8 — Dati personali (§10).** `disiscrivi` tratta un recapito: la registrazione di revoca è già dichiarata
  nel manifesto (`consent.record`), e la nuova tabella `tool_confirmation` va aggiunta al manifesto in italiano e
  inglese, a `exportData` e a `purgeData`. Il riepilogo della conferma **non** conserva il recapito in chiaro
  oltre la scadenza della conferma stessa: conserva l'identificativo dell'iscritto.
- **RT-9 — Registrazione eventi (§14).** «Bozza creata da strumento», «verifica pre-volo richiesta da strumento»,
  «conferma usata», «revoca registrata da strumento», con `tenant_id`, `app_id`, `user_id`, identificativo di
  correlazione e identificativo della risorsa; **mai** recapiti né testi.

## 4. Criteri di accettazione

**CA-1 — La bozza non può partire**
- **Dato** una chiamata a `crea_bozza_di_campagna` con tutti i parametri
- **Quando** la campagna viene creata
- **Allora** è nello stato `bozza`, e nessuna combinazione di parametri dello strumento la porta oltre

**CA-2 — La verifica non programma**
- **Dato** una campagna in bozza pronta e un momento futuro
- **Quando** si chiama `programma_invio`
- **Allora** si riceve l'esito del controllo pre-volo con destinatari, esclusi e invii previsti, e la campagna è
  **ancora** nello stato precedente: nulla è stato programmato

**CA-3 — Solo la conferma programma**
- **Dato** l'esito di una verifica con la sua richiesta di conferma
- **Quando** si richiama lo strumento portando il riferimento della conferma
- **Allora** la campagna passa a `programmata`, e la tabella delle conferme registra chi ha autorizzato, quando e
  su che cosa

**CA-4 — Conferma scaduta o riusata**
- **Dato** una conferma scaduta, oppure già usata una volta
- **Quando** la si presenta di nuovo
- **Allora** non produce alcun effetto e lo strumento chiede di rifare la verifica

**CA-5 — Disiscrizione irreversibile con conferma**
- **Dato** un iscritto contattabile
- **Quando** si chiama `disiscrivi` senza conferma
- **Allora** si riceve il riepilogo di ciò che accadrà e nulla cambia; con la conferma, la revoca è registrata e
  l'iscritto non è più contattabile

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, e un recapito che esiste solo in `B`
- **Quando** un utente di `A` chiama `disiscrivi` su quel recapito
- **Allora** riceve la stessa risposta di un recapito inesistente e in `B` non cambia nulla

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sul ciclo di vita della conferma (validità breve, uso singolo) e di **integrazione** sui
      tre strumenti, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su tutti e tre gli strumenti, compreso il caso del recapito altrui;
- [ ] **prova end-to-end**: coprire ora — il percorso `[J-CAMPAIGNS]` (storia 0037) esercita il ciclo
      bozza-conferma su `crea_bozza_di_campagna` e `programma_invio`; voce aggiunta al registro di copertura;
- [ ] **traduzioni** dei testi di riepilogo delle conferme presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `tool_confirmation`, con la tabella in
      esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotato perché `programma_invio` non programma e perché
      `registra_consenso` e `importa_lista` non sono esposti;
- [ ] contratto degli **strumenti conversazionali** dichiarato, con la marcatura di reversibilità per ciascuno;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0034` | Riusa la forma del contratto e la catena dei varchi degli strumenti |
| Storia `0018` | `programma_invio` restituisce l'esito del controllo pre-volo, che deve esistere |
| Storia `0012` | `disiscrivi` scrive nello stesso registro della disiscrizione in un clic, e produce lo stesso effetto |
| UC 0061-0063 (livello conversazionale) | Non implementati: il ciclo bozza-conferma si prova chiamando il contratto direttamente |

## 7. Fuori ambito

- la generazione del testo del messaggio: è la storia 0036;
- l'esposizione della registrazione di consenso e dell'importazione: deliberatamente esclusa (RF-6);
- l'esposizione della scelta del vincitore in una prova a due varianti: esclusa nella storia 0031;
- il consenso delegato all'assistente e i limiti di frequenza per chiamante: sono di piattaforma (UC 0062, 0064).

## 8. Punti aperti

- **Durata di validità di una conferma.** Proposta: pochi minuti. Troppo corta rende la chat inservibile, troppo
  lunga trasforma la conferma in una firma in bianco. Chiude lo sviluppatore, insieme alla regola comune che la
  piattaforma darà a tutte le app quando il livello conversazionale esisterà.
